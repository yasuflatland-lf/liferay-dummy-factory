package com.liferay.support.tools.workflow.adapter.content;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowParameterValuesTest {

	@Test
	void optionalBooleanAcceptsBooleansAndExactBooleanStrings() {
		WorkflowParameterValues workflowParameterValues =
			new WorkflowParameterValues(
				Map.of(
					"booleanValue", Boolean.TRUE, "falseString", "false",
					"trueString", "true"));

		assertTrue(
			workflowParameterValues.optionalBoolean("booleanValue", false));
		assertTrue(workflowParameterValues.optionalBoolean("trueString", false));
		assertFalse(
			workflowParameterValues.optionalBoolean("falseString", true));
	}

	@Test
	void optionalBooleanRejectsInvalidStrings() {
		WorkflowParameterValues workflowParameterValues =
			new WorkflowParameterValues(Map.of("flag", "yes"));

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> workflowParameterValues.optionalBoolean("flag", false));

		assertEquals("flag must be a boolean", exception.getMessage());
	}

	@Test
	void optionalStringRejectsNonStrings() {
		WorkflowParameterValues workflowParameterValues =
			new WorkflowParameterValues(Map.of("title", 123));

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> workflowParameterValues.optionalString("title", "default"));

		assertEquals("title must be a string", exception.getMessage());
	}

	@Test
	void requirePositiveLongArrayRequiresNamedArrayInput() {
		WorkflowParameterValues workflowParameterValues =
			new WorkflowParameterValues(Map.of("groupId", new long[] {1L, 2L}));

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> workflowParameterValues.requirePositiveLongArray("groupIds"));

		assertEquals("groupIds is required", exception.getMessage());
	}

	@Test
	void requirePositiveLongArrayAcceptsExactNamedArrayInput() {
		WorkflowParameterValues workflowParameterValues =
			new WorkflowParameterValues(Map.of("groupIds", new long[] {1L, 2L}));

		assertArrayEquals(
			new long[] {1L, 2L},
			workflowParameterValues.requirePositiveLongArray("groupIds"));
	}

}
