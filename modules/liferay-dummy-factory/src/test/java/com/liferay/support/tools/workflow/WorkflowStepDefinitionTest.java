package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowStepDefinitionTest {

	@Test
	void constructorRejectsNullParameters() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepDefinition(
				"step-1", "sample.operation", "idem-1", null,
				WorkflowErrorPolicy.FAIL_FAST));

		assertEquals("parameters is required", exception.getMessage());
	}

	@Test
	void constructorRejectsUnsupportedStepIdCharacters() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepDefinition(
				"step.site", "sample.operation", "idem-1", List.of(),
				WorkflowErrorPolicy.FAIL_FAST));

		assertEquals(
			"step id must contain only letters, digits, hyphens, or underscores",
			exception.getMessage());
	}

	@Test
	void constructorRejectsBracketCharacters() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepDefinition(
				"step[0]", "sample.operation", "idem-1", List.of(),
				WorkflowErrorPolicy.FAIL_FAST));

		assertEquals(
			"step id must contain only letters, digits, hyphens, or underscores",
			exception.getMessage());
	}

}
