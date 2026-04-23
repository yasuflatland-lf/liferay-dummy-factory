package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowStepResultTest {

	@Test
	void constructorRejectsMismatchedItemsSize() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				true, 1, 1, 0, null, List.of(), Map.of()));

		assertEquals("count must match items size", exception.getMessage());
	}

	@Test
	void constructorRejectsMismatchedRequestedCount() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				false, 2, 1, 0, "broken", List.of(Map.of()), Map.of()));

		assertEquals("count + skipped must equal requested", exception.getMessage());
	}

	@Test
	void constructorRejectsSuccessWithError() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				true, 1, 1, 0, "broken", List.of(Map.of()), Map.of()));

		assertEquals(
			"error must be null when success is true", exception.getMessage());
	}

	@Test
	void constructorRejectsFailureWithoutError() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				false, 1, 1, 0, null, List.of(Map.of()), Map.of()));

		assertEquals(
			"error is required when success is false", exception.getMessage());
	}

}
