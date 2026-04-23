package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BatchResultTest {

	@Test
	void successFactory_countEqualsItemsSize() {
		BatchResult<String> result = BatchResult.success(
			3, List.of("a", "b", "c"), 0);

		assertTrue(result.success());
		assertEquals(3, result.count());
		assertEquals(3, result.requested());
		assertEquals(0, result.skipped());
		assertNull(result.error());
	}

	@Test
	void successFactory_countNotEqualRequested_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> BatchResult.success(5, List.of("a", "b", "c"), 0));
	}

	@Test
	void constructor_successTrueCountNotRequested_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(true, 2, 5, 0, List.of("a", "b"), null));
	}

	@Test
	void failureFactory_setsSuccessFalseAndError() {
		BatchResult<String> result = BatchResult.failure(
			3, List.of("a"), 0, "Only 1 of 3 created.");

		assertFalse(result.success());
		assertEquals(1, result.count());
		assertEquals(3, result.requested());
		assertEquals("Only 1 of 3 created.", result.error());
	}

	@Test
	void failureFactory_nullItemsProducesEmptyList() {
		BatchResult<String> result = BatchResult.failure(
			3, null, 0, "error message");

		assertFalse(result.success());
		assertTrue(result.items().isEmpty());
	}

	@Test
	void constructor_successFalseNullError_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(false, 0, 3, 0, List.of(), null));
	}

	@Test
	void constructor_successFalseBlankError_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(false, 0, 3, 0, List.of(), "  "));
	}

	@Test
	void constructor_successFalseValidError_doesNotThrow() {
		assertDoesNotThrow(
			() -> new BatchResult<>(false, 0, 3, 0, List.of(), "error"));
	}

	@Test
	void constructor_successTrueNullError_doesNotThrow() {
		assertDoesNotThrow(
			() -> new BatchResult<>(true, 3, 3, 0, List.of("a","b","c"), null));
	}

	@Test
	void constructor_requestedZero_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(true, 0, 0, 0, List.of(), null));
	}

	@Test
	void constructor_requestedNegative_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(true, 0, -1, 0, List.of(), null));
	}

	@Test
	void constructor_countNegative_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(true, -1, 1, 0, List.of(), null));
	}

	@Test
	void constructor_skippedNegative_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new BatchResult<>(true, 0, 1, -1, List.of(), null));
	}

	@Test
	void constructor_nullItems_becomesEmptyList() {
		BatchResult<String> result = new BatchResult<>(
			false, 0, 1, 0, null, "error");

		assertTrue(result.items().isEmpty());
	}

	@Test
	void recordEquality_works() {
		assertEquals(
			BatchResult.success(2, List.of("a", "b"), 0),
			BatchResult.success(2, List.of("a", "b"), 0));
	}

}
