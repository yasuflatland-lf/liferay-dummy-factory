package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowStepResultTest {

	@Test
	void constructorAcceptsCountGreaterThanItemsSize() {
		List<Map<String, Object>> items = List.of(
			Map.of("groupId", 201L, "siteName", "Site", "created", 3, "failed", 0));
		WorkflowStepResult result = new WorkflowStepResult(
			true, 3, 3, 0, null, items, Map.of());

		assertEquals(3, result.count());
		assertEquals(1, result.items().size());
		assertEquals(
			Map.of(
				"success", true, "requested", 3, "count", 3, "skipped", 0,
				"items", items),
			result.asMap());
		assertFalse(result.asMap().containsKey("error"));
	}

	@Test
	void constructorAcceptsCountLessThanItemsSizeOnFailure() {
		List<Map<String, Object>> items = List.of(
			Map.of("groupId", 201L, "created", 0, "failed", 1, "error", "x"),
			Map.of("groupId", 202L, "created", 0, "failed", 1, "error", "x"));
		WorkflowStepResult result = new WorkflowStepResult(
			false, 2, 0, 2, "x", items, Map.of());

		assertEquals(0, result.count());
		assertEquals(2, result.items().size());
		assertEquals(
			Map.of(
				"success", false, "requested", 2, "count", 0, "skipped", 2,
				"items", items, "error", "x"),
			result.asMap());
	}

	@Test
	void constructorRejectsSuccessWithIncompleteCount() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				true, 3, 2, 1, null, List.of(Map.of()), Map.of()));

		assertEquals(
			"success requires count to equal requested", exception.getMessage());
	}

	@Test
	void constructorRejectsNegativeRequested() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				false, -1, 0, 0, "x", List.of(), Map.of()));

		assertEquals(
			"requested must be greater than or equal to 0", exception.getMessage());
	}

	@Test
	void constructorRejectsNegativeCount() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				false, 1, -1, 2, "x", List.of(), Map.of()));

		assertEquals(
			"count must be greater than or equal to 0", exception.getMessage());
	}

	@Test
	void constructorRejectsNegativeSkipped() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult(
				false, 1, 2, -1, "x", List.of(), Map.of()));

		assertEquals(
			"skipped must be greater than or equal to 0", exception.getMessage());
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
