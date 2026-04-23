package com.liferay.support.tools.workflow.jaxrs;

import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.workflow.DefaultWorkflowFunction;
import com.liferay.support.tools.workflow.MapWorkflowFunctionRegistry;
import com.liferay.support.tools.workflow.WorkflowEngine;
import com.liferay.support.tools.workflow.WorkflowErrorPolicy;
import com.liferay.support.tools.workflow.WorkflowFunction;
import com.liferay.support.tools.workflow.WorkflowFunctionDescriptor;
import com.liferay.support.tools.workflow.WorkflowFunctionFactory;
import com.liferay.support.tools.workflow.WorkflowFunctionParameter;
import com.liferay.support.tools.workflow.WorkflowLiteralValue;
import com.liferay.support.tools.workflow.WorkflowParameter;
import com.liferay.support.tools.workflow.WorkflowPlan;
import com.liferay.support.tools.workflow.WorkflowReferenceParser;
import com.liferay.support.tools.workflow.WorkflowReferenceValue;
import com.liferay.support.tools.workflow.WorkflowStepDefinition;
import com.liferay.support.tools.workflow.WorkflowValidationError;
import com.liferay.support.tools.workflow.adapter.taxonomy.CategoryCreateWorkflowOperationAdapter;
import com.liferay.support.tools.workflow.adapter.taxonomy.VocabularyCreateWorkflowOperationAdapter;
import com.liferay.support.tools.service.CategoryCreator;
import com.liferay.support.tools.service.VocabularyCreator;
import com.liferay.support.tools.workflow.dto.WorkflowExecuteResponseDto;
import com.liferay.support.tools.workflow.dto.WorkflowOnErrorDto;
import com.liferay.support.tools.workflow.dto.WorkflowParameterDto;
import com.liferay.support.tools.workflow.dto.WorkflowPlanResponseDto;
import com.liferay.support.tools.workflow.dto.WorkflowRequestDto;
import com.liferay.support.tools.workflow.dto.WorkflowStepDto;
import com.liferay.support.tools.workflow.dto.WorkflowValidationErrorDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
	property = {
		"osgi.jaxrs.application.select=(osgi.jaxrs.name=ldf-workflow)",
		"osgi.jaxrs.resource=true"
	},
	service = Object.class
)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowResource {

	private static final Pattern _STEP_ID_PATTERN = Pattern.compile(
		"[A-Za-z0-9_-]+");

	@POST
	@Path("execute")
	public WorkflowExecuteResponseDto execute(
		@Context HttpServletRequest httpServletRequest,
		WorkflowRequestDto workflowRequestDto) {

		ValidationResult validationResult = _validatedPlan(workflowRequestDto);

		if (!validationResult.errors().isEmpty()) {
			return new WorkflowExecuteResponseDto(null, validationResult.errors());
		}

		return new WorkflowExecuteResponseDto(
			_workflowEngine().execute(
				validationResult.plan(), _currentUserId(httpServletRequest),
				_currentCompanyId(httpServletRequest)),
			List.of());
	}

	@GET
	@Path("functions")
	public Map<String, Object> functions() {
		Map<String, Object> document = new LinkedHashMap<>();
		List<Map<String, Object>> functions = new ArrayList<>();

		for (WorkflowFunction workflowFunction : _workflowFunctions().values()) {
			functions.add(_functionDocument(workflowFunction));
		}

		document.put("basePath", "/o/ldf-workflow");
		document.put("endpoint", "/functions");
		document.put("functions", List.copyOf(functions));
		document.put("referenceSyntax", _referenceSyntax());

		return Map.copyOf(document);
	}

	@POST
	@Path("plan")
	public WorkflowPlanResponseDto plan(WorkflowRequestDto workflowRequestDto) {
		ValidationResult validationResult = _validatedPlan(workflowRequestDto);

		return new WorkflowPlanResponseDto(
			validationResult.plan(), validationResult.errors());
	}

	@GET
	@Path("schema")
	public Map<String, Object> schema() {
		Map<String, Object> document = new LinkedHashMap<>();

		document.put("basePath", "/o/ldf-workflow");
		document.put("endpoint", "/schema");
		document.put("schema", _schemaDocument());
		document.put("referenceSyntax", _referenceSyntax());
		document.put(
			"notes",
			List.of(
				"`from` supports `input.*` and `steps.<stepId>.*` references.",
				"Steps execute from top to bottom.",
				"`execute` stops at the first failing step because only FAIL_FAST is supported right now."));

		return Map.copyOf(document);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindSpiWorkflowOperationAdapter(
		com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter
			workflowOperationAdapter) {

		_spiWorkflowOperationAdapters.put(
			workflowOperationAdapter.operationName(), workflowOperationAdapter);
	}

	protected void unbindSpiWorkflowOperationAdapter(
		com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter
			workflowOperationAdapter) {

		_spiWorkflowOperationAdapters.remove(workflowOperationAdapter.operationName());
	}

	private long _currentCompanyId(HttpServletRequest httpServletRequest) {
		if ((_portal == null) || (httpServletRequest == null)) {
			return 0L;
		}

		return _portal.getCompanyId(httpServletRequest);
	}

	private long _currentUserId(HttpServletRequest httpServletRequest) {
		if ((_portal == null) || (httpServletRequest == null)) {
			return 0L;
		}

		return _portal.getUserId(httpServletRequest);
	}

	private Map<String, Object> _functionDocument(WorkflowFunction workflowFunction) {
		Map<String, Object> document = new LinkedHashMap<>();

		document.put("operation", workflowFunction.operation());

		if (workflowFunction instanceof DefaultWorkflowFunction defaultWorkflowFunction) {
			WorkflowFunctionDescriptor workflowFunctionDescriptor =
				defaultWorkflowFunction.descriptor();

			document.put("description", workflowFunctionDescriptor.description());
			document.put("parameters", _parameterDocuments(workflowFunctionDescriptor));
			document.put("resultShape", workflowFunctionDescriptor.resultShape());
		}
		else {
			document.put("description", workflowFunction.operation());
			document.put("parameters", List.of());
			document.put("resultShape", "WorkflowStepResult");
		}

		return Map.copyOf(document);
	}

	private List<Map<String, Object>> _parameterDocuments(
		WorkflowFunctionDescriptor workflowFunctionDescriptor) {

		List<Map<String, Object>> parameters = new ArrayList<>();

		for (WorkflowFunctionParameter workflowFunctionParameter :
				workflowFunctionDescriptor.parameters()) {

			Map<String, Object> parameter = new LinkedHashMap<>();

			parameter.put("name", workflowFunctionParameter.name());
			parameter.put("type", workflowFunctionParameter.type());
			parameter.put("required", workflowFunctionParameter.required());
			parameter.put("description", workflowFunctionParameter.description());

			if (workflowFunctionParameter.defaultValue() != null) {
				parameter.put("default", workflowFunctionParameter.defaultValue());
			}

			parameters.add(Map.copyOf(parameter));
		}

		return List.copyOf(parameters);
	}

	private List<Map<String, Object>> _referenceSyntax() {
		return List.of(
			Map.of(
				"pattern", "input[.<property>[.<nestedProperty>|[index]>...]]",
				"example", "input.groupId",
				"description", "Read a value from workflow input."),
			Map.of(
				"pattern", "steps.<stepId>[.<property>[.<nestedProperty>|[index]>...]]",
				"example", "steps.createSite.items[0].groupId",
				"description", "Read a value from an earlier step result."));
	}

	private Map<String, Object> _schemaDocument() {
		List<String> operations = new ArrayList<>(_workflowFunctions().keySet());
		operations.sort(Comparator.naturalOrder());

		Map<String, Object> parameterSchema = new LinkedHashMap<>();

		parameterSchema.put(
			"oneOf",
			List.of(
				Map.of(
					"type", "object",
					"required", List.of("name", "value"),
					"properties",
					Map.of(
						"name", Map.of("type", "string"),
						"value", Map.of()),
					"additionalProperties", false),
				Map.of(
					"type", "object",
					"required", List.of("name", "from"),
					"properties",
					Map.of(
						"name", Map.of("type", "string"),
						"from",
						Map.of(
							"type", "string",
							"pattern",
							"^(input(\\.[A-Za-z_][A-Za-z0-9_-]*(\\.[A-Za-z_][A-Za-z0-9_-]*|\\[[0-9]+\\])*)?|steps\\.[A-Za-z0-9_-]+(\\.[A-Za-z_][A-Za-z0-9_-]*(\\.[A-Za-z_][A-Za-z0-9_-]*|\\[[0-9]+\\])*)?)$")),
					"additionalProperties", false)));

		Map<String, Object> schema = new LinkedHashMap<>();

		schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
		schema.put("$id", "urn:liferay:ldf-workflow:request");
		schema.put("type", "object");
		schema.put("required", List.of("schemaVersion", "steps"));
		schema.put("additionalProperties", false);
		schema.put(
			"properties",
			Map.of(
				"schemaVersion",
				Map.of(
					"type", "string",
					"const", "1.0"),
				"workflowId", Map.of("type", "string"),
				"input", Map.of("type", "object"),
				"steps",
				Map.of(
					"type", "array",
					"minItems", 1,
					"items",
					Map.of(
						"type", "object",
						"required", List.of("id", "operation", "idempotencyKey"),
						"properties",
						Map.of(
							"id", Map.of("type", "string"),
							"operation", Map.of("type", "string", "enum", operations),
							"idempotencyKey", Map.of("type", "string"),
							"params",
							Map.of("type", "array", "items", parameterSchema),
							"onError",
							Map.of(
								"type", "object",
								"properties",
								Map.of(
									"policy",
									Map.of(
										"type", "string",
										"enum", List.of(WorkflowErrorPolicy.FAIL_FAST.name()))),
								"additionalProperties", false)),
						"additionalProperties", false))));

		return Map.copyOf(schema);
	}

	private WorkflowEngine _workflowEngine() {
		return new WorkflowEngine(
			new MapWorkflowFunctionRegistry(_workflowFunctions()));
	}

	private Map<String, WorkflowFunction> _workflowFunctions() {
		Map<String, WorkflowFunction> workflowFunctions = new LinkedHashMap<>();

		for (com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter
				workflowOperationAdapter : _spiWorkflowOperationAdapters.values()) {

			workflowFunctions.put(
				workflowOperationAdapter.operationName(),
				_workflowFunctionFactory.create(workflowOperationAdapter));
		}

		// The taxonomy adapters were the only operations observed to be
		// intermittently unavailable during bundle startup in the integration
		// test environment. Keep the fallback narrowly scoped so we preserve the
		// normal OSGi registration path for all other workflow operations.
		_addFallbackWorkflowFunction(
			workflowFunctions,
			new VocabularyCreateWorkflowOperationAdapter(_vocabularyCreator));
		_addFallbackWorkflowFunction(
			workflowFunctions,
			new CategoryCreateWorkflowOperationAdapter(_categoryCreator));

		return workflowFunctions.entrySet().stream(
		).sorted(
			Map.Entry.comparingByKey()
		).collect(
			LinkedHashMap::new,
			(map, entry) -> map.put(entry.getKey(), entry.getValue()),
			Map::putAll
		);
	}

	private void _addFallbackWorkflowFunction(
		Map<String, WorkflowFunction> workflowFunctions,
		com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter
			workflowOperationAdapter) {

		// Only fill gaps. If the adapter is already registered through OSGi,
		// leave that instance in place so the fallback never overrides normal
		// component wiring or hides a wider registration problem.
		workflowFunctions.putIfAbsent(
			workflowOperationAdapter.operationName(),
			_workflowFunctionFactory.create(workflowOperationAdapter));
	}

	private WorkflowPlan _toPlan(WorkflowRequestDto workflowRequestDto) {
		List<WorkflowStepDefinition> stepDefinitions = new ArrayList<>();

		for (WorkflowStepDto workflowStepDto : workflowRequestDto.steps()) {
			stepDefinitions.add(
				new WorkflowStepDefinition(
					workflowStepDto.id(), workflowStepDto.operation(),
					workflowStepDto.idempotencyKey(), _toParameters(workflowStepDto),
					_onErrorPolicy(workflowStepDto.onError())));
		}

		return new WorkflowPlan(
			new com.liferay.support.tools.workflow.WorkflowDefinition(
				workflowRequestDto.schemaVersion(), workflowRequestDto.workflowId(),
				(workflowRequestDto.input() == null) ? Map.of() :
					Collections.unmodifiableMap(
						new LinkedHashMap<>(workflowRequestDto.input())),
				stepDefinitions));
	}

	private List<WorkflowParameter> _toParameters(WorkflowStepDto workflowStepDto) {
		if ((workflowStepDto.params() == null) || workflowStepDto.params().isEmpty()) {
			return List.of();
		}

		List<WorkflowParameter> workflowParameters = new ArrayList<>();

		for (WorkflowParameterDto workflowParameterDto : workflowStepDto.params()) {
			workflowParameters.add(
				new WorkflowParameter(
					workflowParameterDto.name(),
					((workflowParameterDto.from() != null) &&
					 !workflowParameterDto.from().isBlank()) ?
						new WorkflowReferenceValue(
							_workflowReferenceParser.parse(workflowParameterDto.from())) :
						new WorkflowLiteralValue(workflowParameterDto.value())));
		}

		return List.copyOf(workflowParameters);
	}

	private List<WorkflowValidationErrorDto> _toValidationErrorDtos(
		List<WorkflowValidationError> workflowValidationErrors) {

		List<WorkflowValidationErrorDto> workflowValidationErrorDtos =
			new ArrayList<>(workflowValidationErrors.size());

		for (WorkflowValidationError workflowValidationError :
				workflowValidationErrors) {

			workflowValidationErrorDtos.add(
				new WorkflowValidationErrorDto(
					workflowValidationError.code(), workflowValidationError.path(),
					workflowValidationError.message()));
		}

		return List.copyOf(workflowValidationErrorDtos);
	}

	private ValidationResult _validatedPlan(WorkflowRequestDto workflowRequestDto) {
		List<WorkflowValidationErrorDto> errors = _validateRequest(workflowRequestDto);

		if (!errors.isEmpty()) {
			return new ValidationResult(null, errors);
		}

		WorkflowPlan workflowPlan = _toPlan(workflowRequestDto);
		List<WorkflowValidationErrorDto> semanticErrors = _toValidationErrorDtos(
			_workflowEngine().validate(workflowPlan));

		if (!semanticErrors.isEmpty()) {
			return new ValidationResult(workflowPlan, semanticErrors);
		}

		return new ValidationResult(workflowPlan, List.of());
	}

	private List<WorkflowValidationErrorDto> _validateRequest(
		WorkflowRequestDto workflowRequestDto) {

		List<WorkflowValidationErrorDto> errors = new ArrayList<>();

		if (workflowRequestDto == null) {
			errors.add(
				new WorkflowValidationErrorDto(
					"REQUEST_REQUIRED", "/",
					"Workflow request is required."));

			return List.copyOf(errors);
		}

		if ((workflowRequestDto.schemaVersion() == null) ||
			workflowRequestDto.schemaVersion().isBlank()) {

			errors.add(
				new WorkflowValidationErrorDto(
					"SCHEMA_VERSION_REQUIRED", "/schemaVersion",
					"schemaVersion is required."));
		}
		else if (!workflowRequestDto.schemaVersion().equals("1.0")) {
			errors.add(
				new WorkflowValidationErrorDto(
					"SCHEMA_VERSION_UNSUPPORTED", "/schemaVersion",
					"Unsupported schemaVersion: " +
						workflowRequestDto.schemaVersion()));
		}

		if ((workflowRequestDto.steps() == null) || workflowRequestDto.steps().isEmpty()) {
			errors.add(
				new WorkflowValidationErrorDto(
					"STEPS_REQUIRED", "/steps", "At least one step is required."));

			return List.copyOf(errors);
		}

		for (int i = 0; i < workflowRequestDto.steps().size(); i++) {
			WorkflowStepDto workflowStepDto = workflowRequestDto.steps().get(i);

			if (workflowStepDto == null) {
				errors.add(
					new WorkflowValidationErrorDto(
						"STEP_REQUIRED", "/steps/" + i,
						"A workflow step is required."));

				continue;
			}

			if ((workflowStepDto.id() == null) || workflowStepDto.id().isBlank()) {
				errors.add(
					new WorkflowValidationErrorDto(
						"STEP_ID_REQUIRED", "/steps/" + i + "/id",
						"Step id is required."));
			}
			else if (!_STEP_ID_PATTERN.matcher(workflowStepDto.id()).matches()) {
				errors.add(
					new WorkflowValidationErrorDto(
						"STEP_ID_INVALID", "/steps/" + i + "/id",
						"Step id must contain only letters, digits, hyphens, or underscores."));
			}

			if ((workflowStepDto.operation() == null) ||
				workflowStepDto.operation().isBlank()) {

				errors.add(
					new WorkflowValidationErrorDto(
						"STEP_OPERATION_REQUIRED", "/steps/" + i + "/operation",
						"Step operation is required."));
			}

			if ((workflowStepDto.idempotencyKey() == null) ||
				workflowStepDto.idempotencyKey().isBlank()) {

				errors.add(
					new WorkflowValidationErrorDto(
						"STEP_IDEMPOTENCY_KEY_REQUIRED",
						"/steps/" + i + "/idempotencyKey",
						"Step idempotencyKey is required."));
			}

			if (workflowStepDto.params() == null) {
				continue;
			}

			for (int j = 0; j < workflowStepDto.params().size(); j++) {
				WorkflowParameterDto workflowParameterDto =
					workflowStepDto.params().get(j);

				if (workflowParameterDto == null) {
					errors.add(
						new WorkflowValidationErrorDto(
							"STEP_PARAMETER_REQUIRED", "/steps/" + i + "/params/" + j,
							"A workflow parameter is required."));

					continue;
				}

				if ((workflowParameterDto.name() == null) ||
					workflowParameterDto.name().isBlank()) {

					errors.add(
						new WorkflowValidationErrorDto(
							"STEP_PARAMETER_NAME_REQUIRED",
							"/steps/" + i + "/params/" + j + "/name",
						"Parameter name is required."));
				}

				boolean hasFrom = (workflowParameterDto.from() != null) &&
					!workflowParameterDto.from().isBlank();

				if (!hasFrom) {
					continue;
				}

				try {
					_workflowReferenceParser.parse(workflowParameterDto.from());
				}
				catch (IllegalArgumentException illegalArgumentException) {
					errors.add(
						new WorkflowValidationErrorDto(
							"STEP_PARAMETER_FROM_INVALID",
							"/steps/" + i + "/params/" + j + "/from",
							illegalArgumentException.getMessage()));
				}
			}
		}

		return List.copyOf(errors);
	}

	private WorkflowErrorPolicy _onErrorPolicy(WorkflowOnErrorDto workflowOnErrorDto) {
		if ((workflowOnErrorDto == null) || (workflowOnErrorDto.policy() == null)) {
			return WorkflowErrorPolicy.FAIL_FAST;
		}

		return workflowOnErrorDto.policy();
	}

	@Reference
	private Portal _portal;

	@Reference
	private CategoryCreator _categoryCreator;

	@Reference
	private VocabularyCreator _vocabularyCreator;

	private final WorkflowFunctionFactory _workflowFunctionFactory =
		new WorkflowFunctionFactory();
	private final WorkflowReferenceParser _workflowReferenceParser =
		new WorkflowReferenceParser();
	private final Map<String, com.liferay.support.tools.workflow.spi.
		WorkflowOperationAdapter> _spiWorkflowOperationAdapters =
			new ConcurrentHashMap<>();

	private record ValidationResult(
		WorkflowPlan plan,
		List<WorkflowValidationErrorDto> errors) {
	}

}
