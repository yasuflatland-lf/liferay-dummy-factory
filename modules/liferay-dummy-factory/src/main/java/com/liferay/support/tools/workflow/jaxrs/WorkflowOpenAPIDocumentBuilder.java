package com.liferay.support.tools.workflow.jaxrs;

import com.liferay.support.tools.service.BatchSpec;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkflowOpenAPIDocumentBuilder {

	public static Map<String, Object> build(
		Map<String, Object> workflowRequestSchema) {

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
					"Runs the steps top to bottom and stops at the first failing step. Returns HTTP 200 even when a step fails: check execution.status (SUCCEEDED or FAILED) and errors in the response body.",
					"Execution result", schema)));

		return _map(
			"openapi", "3.1.0",
			"info",
			_map(
				"title", "Liferay Dummy Factory Workflow", "version", "1.0",
				"description", _INFO_DESCRIPTION),
			"paths", paths);
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

	private static Map<String, Object> _requestBody(Map<String, Object> schema) {
		return _map("required", true, "content", _content(schema));
	}

	private static Map<String, Object> _response(String description) {
		return _map(
			"description", description,
			"content", _content(_map("type", "object")));
	}

	private static final String _INFO_DESCRIPTION = "Creates dummy data (sites, users, organizations, roles, web content, documents, blogs, pages, vocabularies, categories, message boards) in this Liferay instance. Workflow: call getWorkflowFunctions to learn operations, compose a request, validate it with planWorkflow, then run executeWorkflow. Inside a workflow, a step parameter is either {\"name\", \"value\"} or {\"name\", \"from\"}; \"from\" reads input.<field> or an earlier result such as steps.<stepId>.items[0].groupId. Site-scoped operations need groupId > 0: create or look up the site first and pass its groupId. count is limited to " + BatchSpec.MAX_COUNT + " per step; for webContent.create the limit applies to count × groupIds.";

}
