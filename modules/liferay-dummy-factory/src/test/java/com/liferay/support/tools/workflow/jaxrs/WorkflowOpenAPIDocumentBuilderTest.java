package com.liferay.support.tools.workflow.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
			"3.1.0", WorkflowOpenAPIDocumentBuilder.build(_schema()).get("openapi"));
	}

	@Test
	void exposesExactlyFourOperations() {
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(_schema()), "paths");

		assertEquals(
			List.of("/functions", "/schema", "/plan", "/execute"),
			new ArrayList<>(paths.keySet()));
	}

	@Test
	void operationIdsAreStable() {
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(_schema()), "paths");
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
			WorkflowOpenAPIDocumentBuilder.build(input), "paths");

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
			WorkflowOpenAPIDocumentBuilder.build(_schema()), "info");
		String description = (String)info.get("description");

		assertTrue(description.contains("steps.<stepId>"));
		assertTrue(description.contains("1000"));
	}

	@Test
	void documentHasNoRefsOrComponents() {
		Map<String, Object> document = WorkflowOpenAPIDocumentBuilder.build(
			_schema());

		assertFalse(document.containsKey("components"));
		assertFalse(document.toString().contains("$ref"));
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
