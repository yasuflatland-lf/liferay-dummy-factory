package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowDefinitionTest {

	@Test
	void constructorRejectsNullInput() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowDefinition("1.0", "workflow-1", null, List.of()));

		assertEquals("input is required", exception.getMessage());
	}

	@Test
	void constructorRejectsNullSteps() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowDefinition(
				"1.0", "workflow-1", Map.of(), null));

		assertEquals("steps is required", exception.getMessage());
	}

	@Test
	void constructorPreservesNullableInputValues() {
		Map<String, Object> input = new java.util.LinkedHashMap<>();

		input.put("groupId", null);

		WorkflowDefinition workflowDefinition = new WorkflowDefinition(
			"1.0", "workflow-1", input, List.of());

		assertTrue(workflowDefinition.input().containsKey("groupId"));
		assertEquals(null, workflowDefinition.input().get("groupId"));
	}

}
