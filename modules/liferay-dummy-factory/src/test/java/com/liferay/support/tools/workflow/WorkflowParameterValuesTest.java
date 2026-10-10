package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowParameterValuesTest {

	@Test
	void requireCountRejectsAboveMax() {
		WorkflowParameterValues values =
			new WorkflowParameterValues(Map.of("count", 1001));

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			values::requireCount);

		assertEquals(
			"count must be less than or equal to 1000", exception.getMessage());
	}

	@Test
	void requireCountAcceptsMax() {
		WorkflowParameterValues values =
			new WorkflowParameterValues(Map.of("count", 1000));

		assertEquals(1000, values.requireCount());
	}

}
