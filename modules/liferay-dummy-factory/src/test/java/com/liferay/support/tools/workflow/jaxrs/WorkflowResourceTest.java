package com.liferay.support.tools.workflow.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.workflow.dto.WorkflowParameterDto;
import com.liferay.support.tools.workflow.dto.WorkflowRequestDto;
import com.liferay.support.tools.workflow.dto.WorkflowStepDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
