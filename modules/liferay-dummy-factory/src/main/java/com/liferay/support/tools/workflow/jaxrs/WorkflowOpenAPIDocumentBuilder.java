package com.liferay.support.tools.workflow.jaxrs;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.WorkflowFunctionDescriptor;
import com.liferay.support.tools.workflow.WorkflowFunctionParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowOpenAPIDocumentBuilder {

	public static Map<String, Object> build(
		Map<String, Object> workflowRequestSchema,
		List<WorkflowFunctionDescriptor> descriptors) {

		Map<String, Object> schema = new LinkedHashMap<>(workflowRequestSchema);

		schema.remove("$schema");
		schema.remove("$id");

		Map<String, Object> paths = new LinkedHashMap<>();

		paths.put(
			"/functions",
			_map(
				"get",
				_operation(
					"getWorkflowFunctions",
					"List the data-creation operations and their parameters",
					"Returns every operation name with typed parameters, defaults and result shape. Call this before composing a workflow.",
					"Function catalog", null)));
		paths.put(
			"/schema",
			_map(
				"get",
				_operation(
					"getWorkflowSchema", "Get the JSON Schema of a workflow request",
					"Returns the request grammar accepted by planWorkflow and executeWorkflow, including the from-reference syntax.",
					"Workflow request schema", null)));
		paths.put(
			"/plan",
			_map(
				"post",
				_operation(
					"planWorkflow", "Validate a workflow without creating anything",
					"Dry run. Returns the resolved plan and a list of validation errors. Nothing is created. Always call this before executeWorkflow.",
					"Plan and validation errors", schema)));
		paths.put(
			"/execute",
			_map(
				"post",
				_operation(
					"executeWorkflow", "Create data by running a workflow",
					"Runs the steps top to bottom and stops at the first failing step. Returns HTTP 200 even when a step fails: check execution.status (SUCCEEDED or FAILED) and errors in the response body. HTTP 401 when not signed in; HTTP 403 when not a company administrator, or when a step is company.create and the caller is not an omniadmin.",
					"Execution result", schema)));

		List<WorkflowFunctionDescriptor> sortedDescriptors = descriptors.stream(
		).sorted(
			Comparator.comparing(WorkflowFunctionDescriptor::operation)
		).toList();

		for (WorkflowFunctionDescriptor descriptor : sortedDescriptors) {
			String operation = descriptor.operation();
			String operationId = operationIdOf(operation);

			if (operationId == null) {
				continue;
			}

			String summary = descriptor.description();

			if (summary.endsWith(".")) {
				summary = summary.substring(0, summary.length() - 1);
			}

			paths.put(
				"/operations/" + operation,
				_map(
					"post",
					_operation(
						operationId, summary,
						"Runs " + operation + " as a single step. Returns the step result {stepId, operation, status, result: {success, requested, count, skipped, items, error}, error}. HTTP 401 when not signed in; HTTP 403 when not a company administrator" +
							(operation.equals("company.create") ? " or not an omniadmin" : "") +
							". HTTP 400 when a parameter is unknown or a required parameter is missing; HTTP 422 when the step fails, including invalid values such as a count out of range. HTTP 404 when the operation is unknown.",
						"Step succeeded", _parameterSchema(descriptor))));
		}

		return _map(
			"openapi", "3.1.0",
			"info",
			_map(
				"title", "Liferay Dummy Factory Workflow", "version", "1.0",
				"description", _INFO_DESCRIPTION),
			"paths", paths);
	}

	public static String operationIdOf(String operation) {
		return (operation == null) ? null : _OPERATION_IDS.get(operation);
	}

	private WorkflowOpenAPIDocumentBuilder() {
	}

	private static Map<String, Object> _content(Map<String, Object> schema) {
		return _map("application/json", _map("schema", schema));
	}

	private static Map<String, Object> _map(Object... entries) {
		Map<String, Object> map = new LinkedHashMap<>();

		for (int i = 0; i < entries.length; i += 2) {
			map.put((String)entries[i], entries[i + 1]);
		}

		return map;
	}

	private static Map<String, Object> _operation(
		String operationId, String summary, String description,
		String responseDescription, Map<String, Object> schema) {

		Map<String, Object> operation = _map(
			"operationId", operationId, "summary", summary,
			"description", description);

		if (schema != null) {
			operation.put("requestBody", _requestBody(schema));
		}

		operation.put("responses", _map("200", _response(responseDescription)));

		return operation;
	}

	private static Map<String, Object> _parameterSchema(
		WorkflowFunctionDescriptor descriptor) {

		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();

		for (WorkflowFunctionParameter parameter : descriptor.parameters()) {
			Map<String, Object> property = switch (parameter.type()) {
				case "string" -> _map("type", "string");
				case "integer" -> _map("type", "integer", "format", "int32");
				case "long" -> _map("type", "integer", "format", "int64");
				case "boolean" -> _map("type", "boolean");
				case "long[]" -> _map(
					"type", "array", "items",
					_map("type", "integer", "format", "int64"));
				case "string[]" -> _map(
					"type", "array", "items", _map("type", "string"));
				default -> throw new IllegalStateException(
					"Unsupported workflow parameter type: " + parameter.type());
			};

			if (parameter.type().equals("integer") &&
				parameter.name().equals("count")) {

				property.put("minimum", 1);
				property.put("maximum", BatchSpec.MAX_COUNT);
			}

			String description = parameter.description();

			if (parameter.defaultValue() != null) {
				description += " Default: " + parameter.defaultValue() + ".";
			}

			property.put("description", description);
			properties.put(parameter.name(), property);

			if (parameter.required()) {
				required.add(parameter.name());
			}
		}

		Map<String, Object> schema = _map(
			"type", "object", "additionalProperties", false,
			"properties", properties);

		if (!required.isEmpty()) {
			schema.put("required", required);
		}

		return schema;
	}

	private static Map<String, Object> _requestBody(Map<String, Object> schema) {
		return _map("required", true, "content", _content(schema));
	}

	private static Map<String, Object> _response(String description) {
		return _map(
			"description", description,
			"content", _content(_map("type", "object")));
	}

	private static final Map<String, String> _OPERATION_IDS = Map.ofEntries(
		Map.entry("blogs.create", "createBlogsEntries"),
		Map.entry("category.create", "createCategories"),
		Map.entry("company.create", "createCompanies"),
		Map.entry("document.create", "createDocuments"),
		Map.entry("layout.create", "createLayouts"),
		Map.entry("mbCategory.create", "createMBCategories"),
		Map.entry("mbReply.create", "createMBReplies"),
		Map.entry("mbThread.create", "createMBThreads"),
		Map.entry("organization.create", "createOrganizations"),
		Map.entry("role.create", "createRoles"),
		Map.entry("site.create", "createSites"),
		Map.entry("user.create", "createUsers"),
		Map.entry("vocabulary.create", "createVocabularies"),
		Map.entry("webContent.create", "createWebContents"));

	private static final String _INFO_DESCRIPTION = "Creates dummy data (sites, users, organizations, roles, web content, documents, blogs, pages, vocabularies, categories, message boards) in this Liferay instance. Workflow: call getWorkflowFunctions to learn operations, compose a request, validate it with planWorkflow, then run executeWorkflow. Inside a workflow, a step parameter is either {\"name\", \"value\"} or {\"name\", \"from\"}; \"from\" reads input.<field> or an earlier result such as steps.<stepId>.items[0].groupId. Site-scoped operations need groupId > 0: create or look up the site first and pass its groupId. count is limited to " + BatchSpec.MAX_COUNT + " per step; for webContent.create the limit applies to count × groupIds.";

}
