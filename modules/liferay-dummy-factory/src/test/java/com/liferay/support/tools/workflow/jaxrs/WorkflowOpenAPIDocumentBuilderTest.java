package com.liferay.support.tools.workflow.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.WorkflowFunctionDescriptor;
import com.liferay.support.tools.workflow.WorkflowFunctionParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WorkflowOpenAPIDocumentBuilderTest {

	@Test
	void documentIsOpenAPI31() {
		assertEquals(
			"3.1.0",
			WorkflowOpenAPIDocumentBuilder.build(
				_schema(), List.of()).get("openapi"));
	}

	@Test
	void coarsePathsComeFirstInFixedOrder() {
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(_schema(), List.of()), "paths");

		assertEquals(
			List.of("/functions", "/schema", "/plan", "/execute"),
			new ArrayList<>(paths.keySet()));
	}

	@Test
	void operationIdsAreStable() {
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(_schema(), List.of()), "paths");
		Set<Object> operationIds = new HashSet<>();

		for (String path : paths.keySet()) {
			Map<String, Object> methods = _map(paths, path);

			for (String method : methods.keySet()) {
				operationIds.add(_map(methods, method).get("operationId"));
			}
		}

		assertEquals(
			Set.of(
				"getWorkflowFunctions", "getWorkflowSchema", "planWorkflow",
				"executeWorkflow"),
			operationIds);
	}

	@Test
	void requestBodySchemaStripsJsonSchemaMetaKeys() {
		Map<String, Object> input = _schema();
		Map<String, Object> pristine = new LinkedHashMap<>(input);
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(input, List.of()), "paths");

		for (String path : List.of("/plan", "/execute")) {
			Map<String, Object> operation = _map(_map(paths, path), "post");
			Map<String, Object> content = _map(
				_map(operation, "requestBody"), "content");
			Map<String, Object> schema = _map(
				_map(content, "application/json"), "schema");

			assertFalse(schema.containsKey("$schema"));
			assertFalse(schema.containsKey("$id"));
			assertEquals(input.get("required"), schema.get("required"));
			assertEquals(input.get("properties"), schema.get("properties"));
		}

		assertEquals(pristine, input);
	}

	@Test
	void infoDescriptionMentionsReferenceSyntax() {
		Map<String, Object> info = _map(
			WorkflowOpenAPIDocumentBuilder.build(_schema(), List.of()), "info");
		String description = (String)info.get("description");

		assertTrue(description.contains("steps.<stepId>"));
		assertTrue(description.contains("1000"));
	}

	@Test
	void documentHasNoRefsOrComponents() {
		Map<String, Object> document = WorkflowOpenAPIDocumentBuilder.build(
			_schema(), List.of());

		assertFalse(document.containsKey("components"));
		assertFalse(document.toString().contains("$ref"));
	}

	@Test
	void perOperationPathsUseTheFixedOperationIds() {
		Map<String, Object> document = WorkflowOpenAPIDocumentBuilder.build(
			_schema(),
			List.of(_descriptor("user.create"), _descriptor("site.create")));
		Map<String, Object> paths = _map(document, "paths");

		assertEquals(
			"createUsers",
			_map(_map(paths, "/operations/user.create"), "post").get("operationId"));
		assertEquals(
			"createSites",
			_map(_map(paths, "/operations/site.create"), "post").get("operationId"));
		assertEquals(
			List.of(
				"/functions", "/schema", "/plan", "/execute",
				"/operations/site.create", "/operations/user.create"),
			new ArrayList<>(paths.keySet()));
		assertFalse(document.containsKey("components"));
		assertFalse(document.toString().contains("$ref"));
	}

	@Test
	void unmappedOperationIsSkipped() {
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(
				_schema(), List.of(_descriptor("custom.op"))), "paths");

		assertEquals(4, paths.size());
		assertFalse(paths.containsKey("/operations/custom.op"));
		assertNull(WorkflowOpenAPIDocumentBuilder.operationIdOf(null));
		assertNull(WorkflowOpenAPIDocumentBuilder.operationIdOf("custom.op"));
	}

	@Test
	void parameterTypesMapExhaustively() {
		Map<String, Object> schema = _parameterSchema(
			new WorkflowFunctionParameter("text", "string", false, "Text", null),
			new WorkflowFunctionParameter("count", "integer", false, "Count", null),
			new WorkflowFunctionParameter("id", "long", false, "Id", null),
			new WorkflowFunctionParameter("flag", "boolean", false, "Flag", null),
			new WorkflowFunctionParameter("ids", "long[]", false, "Ids", null),
			new WorkflowFunctionParameter("texts", "string[]", false, "Texts", null));
		Map<String, Object> properties = _map(schema, "properties");

		assertEquals(
			Map.of("type", "string", "description", "Text"), properties.get("text"));
		assertEquals(
			Map.of(
				"type", "integer", "format", "int32", "description", "Count",
				"minimum", 1, "maximum", BatchSpec.MAX_COUNT),
			properties.get("count"));
		assertEquals(
			Map.of("type", "integer", "format", "int64", "description", "Id"),
			properties.get("id"));
		assertEquals(
			Map.of("type", "boolean", "description", "Flag"), properties.get("flag"));
		assertEquals(
			Map.of(
				"type", "array", "items", Map.of("type", "integer", "format", "int64"),
				"description", "Ids"),
			properties.get("ids"));
		assertEquals(
			Map.of(
				"type", "array", "items", Map.of("type", "string"),
				"description", "Texts"),
			properties.get("texts"));
		assertEquals(
			List.of("text", "count", "id", "flag", "ids", "texts"),
			new ArrayList<>(properties.keySet()));
		assertEquals("object", schema.get("type"));
		assertEquals(false, schema.get("additionalProperties"));
	}

	@Test
	void otherIntegerParametersHaveNoCountBounds() {
		Map<String, Object> properties = _map(
			_parameterSchema(new WorkflowFunctionParameter(
				"priority", "integer", false, "Priority", null)),
			"properties");

		assertEquals(
			Map.of("type", "integer", "format", "int32", "description", "Priority"),
			properties.get("priority"), "Unexpected schema: " + properties);
	}

	@Test
	void perOperationSummaryAndDescriptionExplainFailures() {
		for (String description : List.of("Create roles.", "Create roles")) {
			Map<String, Object> paths = _map(
				WorkflowOpenAPIDocumentBuilder.build(
					_schema(),
					List.of(new WorkflowFunctionDescriptor(
						"role.create", description, List.of(), "WorkflowStepResult"))),
				"paths");
			Map<String, Object> post = _map(
				_map(paths, "/operations/role.create"), "post");

			assertEquals(
				"Create roles", post.get("summary"), "Unexpected operation: " + post);
			assertEquals(
				"Runs role.create as a single step. Returns the step result {stepId, operation, status, result: {success, requested, count, skipped, items, error}, error}. HTTP 400 when a parameter is unknown or a required parameter is missing; HTTP 422 when the step fails, including invalid values such as a count out of range. HTTP 404 when the operation is unknown.",
				post.get("description"), "Unexpected operation: " + post);
		}
	}

	@Test
	void unknownParameterTypeThrows() {
		IllegalStateException exception = assertThrows(
			IllegalStateException.class,
			() -> _parameterSchema(
				new WorkflowFunctionParameter("date", "date", false, "Date", null)));

		assertEquals(
			"Unsupported workflow parameter type: date", exception.getMessage());
	}

	@Test
	void requiredListMatchesRequiredParameters() {
		WorkflowFunctionParameter optional = new WorkflowFunctionParameter(
			"optional", "string", false, "Optional", null);

		assertEquals(
			List.of("first", "last"),
			_parameterSchema(
				new WorkflowFunctionParameter("first", "string", true, "First", null),
				optional,
				new WorkflowFunctionParameter("last", "integer", true, "Last", null)
			).get("required"));
		assertFalse(_parameterSchema(optional).containsKey("required"));
	}

	@Test
	void noDefaultKeyIsEmitted() {
		Map<String, Object> property = _map(
			_map(
				_parameterSchema(
					new WorkflowFunctionParameter("name", "string", false, "Name", "x")),
				"properties"),
			"name");

		assertFalse(property.containsKey("default"));
		assertEquals("Name Default: x.", property.get("description"));
	}

	private static WorkflowFunctionDescriptor _descriptor(
		String operation, WorkflowFunctionParameter... parameters) {

		return new WorkflowFunctionDescriptor(
			operation, "Description", List.of(parameters), "WorkflowStepResult");
	}

	private static Map<String, Object> _parameterSchema(
		WorkflowFunctionParameter... parameters) {

		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(
				_schema(), List.of(_descriptor("user.create", parameters))), "paths");
		Map<String, Object> post = _map(
			_map(paths, "/operations/user.create"), "post");

		return _map(
			_map(_map(_map(post, "requestBody"), "content"), "application/json"),
			"schema");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> _map(Map<String, Object> map, String key) {
		return (Map<String, Object>)map.get(key);
	}

	private static Map<String, Object> _schema() {
		return Map.of(
			"$schema", "https://json-schema.org/draft/2020-12/schema",
			"$id", "urn:test:workflow", "type", "object",
			"required", List.of("steps"),
			"properties", Map.of("steps", Map.of("type", "array")));
	}

}
