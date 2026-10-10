package com.liferay.support.tools.workflow.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.workflow.WorkflowStepExecutionResult;
import com.liferay.support.tools.workflow.WorkflowStepStatus;
import com.liferay.support.tools.workflow.adapter.core.RoleCreateWorkflowOperationAdapter;
import com.liferay.support.tools.workflow.dto.WorkflowParameterDto;
import com.liferay.support.tools.workflow.dto.WorkflowRequestDto;
import com.liferay.support.tools.workflow.dto.WorkflowStepDto;
import com.liferay.support.tools.workflow.dto.WorkflowValidationErrorDto;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class WorkflowResourceTest {

	@Test
	void planRejectsUnsupportedSchemaVersion() {
		WorkflowResource workflowResource = new WorkflowResource();

		WorkflowRequestDto request = new WorkflowRequestDto(
			"2.0", "wf-1", Map.of(),
			List.of(
				new WorkflowStepDto(
					"step-1", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", 1, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		List<String> errorCodes = workflowResource.plan(
			request
		).errors(
		).stream(
		).map(
			error -> error.code()
		).toList();

		assertTrue(errorCodes.contains("SCHEMA_VERSION_UNSUPPORTED"));
	}

	@Test
	void planAcceptsSchemaVersion10() {
		WorkflowResource workflowResource = new WorkflowResource();

		WorkflowRequestDto request = new WorkflowRequestDto(
			"1.0", "wf-1", Map.of(),
			List.of(
				new WorkflowStepDto(
					"step-1", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", 1, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		List<String> errorCodes = workflowResource.plan(
			request
		).errors(
		).stream(
		).map(
			error -> error.code()
		).toList();

		assertEquals(false, errorCodes.contains("SCHEMA_VERSION_UNSUPPORTED"));
	}

	@Test
	void planAcceptsNullableInputValues() {
		WorkflowResource workflowResource = new WorkflowResource();

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("groupId", null);

		WorkflowRequestDto request = new WorkflowRequestDto(
			"1.0", "wf-1", input,
			List.of(
				new WorkflowStepDto(
					"step-1", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", 1, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		assertNotNull(workflowResource.plan(request));
	}

	@Test
	void planAcceptsExplicitNullParameterValues() {
		WorkflowResource workflowResource = new WorkflowResource();

		WorkflowRequestDto request = new WorkflowRequestDto(
			"1.0", "wf-1", Map.of(),
			List.of(
				new WorkflowStepDto(
					"step-1", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", null, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		assertNotNull(workflowResource.plan(request));
	}

	@Test
	void planRejectsUnsupportedStepIdCharacters() {
		WorkflowResource workflowResource = new WorkflowResource();

		WorkflowRequestDto request = new WorkflowRequestDto(
			"1.0", "wf-1", Map.of(),
			List.of(
				new WorkflowStepDto(
					"bad id", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", 1, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		List<String> errorCodes = workflowResource.plan(
			request
		).errors(
		).stream(
		).map(
			error -> error.code()
		).toList();

		assertTrue(errorCodes.contains("STEP_ID_INVALID"));
	}

	@Test
	void schemaRejectsStepRootIndexInReferencePattern() {
		WorkflowResource workflowResource = new WorkflowResource();

		assertEquals(
			"^(input(\\.[A-Za-z_][A-Za-z0-9_-]*(\\.[A-Za-z_][A-Za-z0-9_-]*|\\[[0-9]+\\])*)?|steps\\.[A-Za-z0-9_-]+(\\.[A-Za-z_][A-Za-z0-9_-]*(\\.[A-Za-z_][A-Za-z0-9_-]*|\\[[0-9]+\\])*)?)$",
			_referencePattern(workflowResource.schema()));
	}

	@Test
	void unknownOperationReturns404() {
		WorkflowResource resource = _resource(_success(), null);
		WorkflowResource.OperationOutcome outcome = resource.executeOperation(
			1L, 1L, "nope.create", Map.of());

		assertEquals(404, outcome.status());
		assertEquals("OPERATION_UNKNOWN", _errors(outcome).get(0).code());
	}

	@Test
	void unknownParametersReturn400WithoutExecuting() {
		AtomicBoolean executed = new AtomicBoolean();
		WorkflowResource resource = new WorkflowResource();

		resource.bindSpiWorkflowOperationAdapter(new WorkflowOperationAdapter() {

			@Override
			public WorkflowStepResult execute(
				WorkflowExecutionContext context, Map<String, Object> parameters) {

				executed.set(true);

				return _success();
			}

			@Override
			public String operationName() {
				return "user.create";
			}
		});

		WorkflowResource.OperationOutcome typoOutcome = resource.executeOperation(
			1L, 1L, "user.create",
			Map.of("count", 1, "baseName", "user", "fakerenable", true));

		assertEquals(400, typoOutcome.status());
		assertEquals(
			List.of(new WorkflowValidationErrorDto(
				"UNKNOWN_PARAMETER", "/fakerenable",
				"Unknown parameter: fakerenable")),
			_errors(typoOutcome));
		assertEquals(false, executed.get());

		Map<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("count", 1);
		parameters.put("baseName", "user");
		parameters.put("userId", 999L);
		parameters.put("companyId", 888L);

		WorkflowResource.OperationOutcome identityOutcome = resource.executeOperation(
			1L, 1L, "user.create", parameters);

		assertEquals(400, identityOutcome.status());
		assertEquals(
			List.of(
				new WorkflowValidationErrorDto(
					"UNKNOWN_PARAMETER", "/companyId", "Unknown parameter: companyId"),
				new WorkflowValidationErrorDto(
					"UNKNOWN_PARAMETER", "/userId", "Unknown parameter: userId")),
			_errors(identityOutcome));
		assertEquals(false, executed.get());
	}

	@Test
	void missingRequiredParameterReturns400() {
		WorkflowResource resource = _resource(_success(), null);
		WorkflowResource.OperationOutcome outcome = resource.executeOperation(
			1L, 1L, "role.create", Map.of("baseName", "role"));

		assertEquals(400, outcome.status());
		assertTrue(!_errors(outcome).isEmpty());
		assertEquals(400, resource.executeOperation(1L, 1L, "role.create", null).status());
	}

	@Test
	void succeededStepReturns200() {
		WorkflowResource.OperationOutcome outcome = _resource(_success(), null).executeOperation(
			1L, 1L, "role.create", Map.of("count", 1, "baseName", "role"));

		assertEquals(200, outcome.status());
		WorkflowStepExecutionResult step = assertInstanceOf(
			WorkflowStepExecutionResult.class, outcome.body());
		assertEquals(WorkflowStepStatus.SUCCEEDED, step.status());
		assertEquals("step", step.stepId());
		assertEquals("role.create", step.operation());
	}

	@Test
	void failedStepReturns422() {
		WorkflowResource.OperationOutcome outcome = _resource(
			new WorkflowStepResult(false, 1, 0, 1, List.of(), "boom"), null
		).executeOperation(
			1L, 1L, "role.create", Map.of("count", 1, "baseName", "role"));

		assertEquals(422, outcome.status());
		WorkflowStepExecutionResult step = assertInstanceOf(
			WorkflowStepExecutionResult.class, outcome.body());
		assertEquals(WorkflowStepStatus.FAILED, step.status());
		assertEquals("boom", step.error().message());
	}

	@Test
	void adapterExceptionReturns422() {
		WorkflowResource.OperationOutcome outcome = _resource(
			null, new IllegalArgumentException("count must be less than or equal to 1000")
		).executeOperation(
			1L, 1L, "role.create", Map.of("count", 1001, "baseName", "role"));

		assertEquals(422, outcome.status());
		WorkflowStepExecutionResult step = assertInstanceOf(
			WorkflowStepExecutionResult.class, outcome.body());
		assertEquals(WorkflowStepStatus.FAILED, step.status());
		assertTrue(step.error().message().contains("1000"));
	}

	@Test
	void zeroCountReturns422() {
		WorkflowResource resource = new WorkflowResource();

		resource.bindSpiWorkflowOperationAdapter(
			new RoleCreateWorkflowOperationAdapter());

		WorkflowResource.OperationOutcome outcome = resource.executeOperation(
			1L, 1L, "role.create", Map.of("count", 0, "baseName", "zero-role"));

		assertEquals(422, outcome.status());
		WorkflowStepExecutionResult step = assertInstanceOf(
			WorkflowStepExecutionResult.class, outcome.body());
		assertEquals(WorkflowStepStatus.FAILED, step.status());
		assertEquals("count must be greater than 0", step.error().message());
	}

	@SuppressWarnings("unchecked")
	private static List<WorkflowValidationErrorDto> _errors(
		WorkflowResource.OperationOutcome outcome) {

		return (List<WorkflowValidationErrorDto>)((Map<?, ?>)outcome.body()).get(
			"errors");
	}

	private static WorkflowResource _resource(
		WorkflowStepResult result, RuntimeException exception) {

		WorkflowResource resource = new WorkflowResource();

		resource.bindSpiWorkflowOperationAdapter(new WorkflowOperationAdapter() {

			@Override
			public WorkflowStepResult execute(
				WorkflowExecutionContext context, Map<String, Object> parameters) {

				assertEquals(1L, context.userId());
				assertEquals(1L, context.companyId());

				if (exception != null) {
					throw exception;
				}

				return result;
			}

			@Override
			public String operationName() {
				return "role.create";
			}

		});

		return resource;
	}

	private static WorkflowStepResult _success() {
		return new WorkflowStepResult(
			true, 1, 1, 0, List.of(Map.of("roleId", 1L)), null);
	}

	@SuppressWarnings("unchecked")
	private static String _referencePattern(Map<String, Object> schema) {
		Map<String, Object> schemaDocument = (Map<String, Object>)schema.get(
			"schema");
		Map<String, Object> properties = (Map<String, Object>)schemaDocument.get(
			"properties");
		Map<String, Object> steps = (Map<String, Object>)properties.get("steps");
		Map<String, Object> items = (Map<String, Object>)steps.get("items");
		Map<String, Object> itemProperties = (Map<String, Object>)items.get(
			"properties");
		Map<String, Object> params = (Map<String, Object>)itemProperties.get(
			"params");
		Map<String, Object> parameterItems = (Map<String, Object>)params.get(
			"items");
		List<Map<String, Object>> oneOf = (List<Map<String, Object>>)parameterItems.get(
			"oneOf");
		Map<String, Object> fromSchema = (Map<String, Object>)oneOf.get(1).get(
			"properties");

		return (String)((Map<String, Object>)fromSchema.get("from")).get(
			"pattern");
	}

}
