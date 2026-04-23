package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowValidationExceptionTest {

	@Test
	void constructorFormatsFirstError() {
		WorkflowValidationException exception = new WorkflowValidationException(
			List.of(
				new WorkflowValidationError(
					"missing_field", "steps[0].id", "step id is required"),
				new WorkflowValidationError(
					"missing_field", "workflowId", "workflow id is required")));

		assertEquals(
			"workflow validation failed: steps[0].id step id is required",
			exception.getMessage());
		assertEquals(2, exception.getErrors().size());
	}

	@Test
	void constructorDefaultsToGenericMessageForNullErrors() {
		WorkflowValidationException exception = new WorkflowValidationException(
			null);

		assertEquals("workflow validation failed", exception.getMessage());
		assertEquals(List.of(), exception.getErrors());
	}

}
