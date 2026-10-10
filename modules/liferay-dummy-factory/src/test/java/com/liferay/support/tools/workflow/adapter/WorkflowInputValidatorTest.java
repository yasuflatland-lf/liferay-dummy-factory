package com.liferay.support.tools.workflow.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorkflowInputValidatorTest {

	@Test
	void requireCountRejectsAboveMax() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> WorkflowInputValidator.requireCount(1001));

		assertEquals(
			"count must be less than or equal to 1000", exception.getMessage());
	}

	@Test
	void requireCountAcceptsMax() {
		assertEquals(1000, WorkflowInputValidator.requireCount(1000));
	}

}
