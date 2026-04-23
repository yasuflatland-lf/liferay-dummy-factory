package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WebContentPerSiteResultTest {

	@Test
	void constructor_validSuccess_passes() {
		assertDoesNotThrow(
			() -> new WebContentPerSiteResult(1L, "Site A", 5, 0, null));
	}

	@Test
	void constructor_validFailure_passes() {
		assertDoesNotThrow(
			() -> new WebContentPerSiteResult(
				1L, "Site A", 3, 2, "Structure not found"));
	}

	@Test
	void constructor_groupIdZero_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(0L, "Site A", 5, 0, null));
	}

	@Test
	void constructor_groupIdNegative_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(-1L, "Site A", 5, 0, null));
	}

	@Test
	void constructor_negativeCreated_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(1L, "Site A", -1, 0, null));
	}

	@Test
	void constructor_negativeFailed_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(1L, "Site A", 0, -1, null));
	}

	@Test
	void constructor_nullSiteName_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(1L, null, 5, 0, null));
	}

	@Test
	void constructor_errorWithoutFailure_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(
				1L, "Site A", 5, 0, "some error"));
	}

	@Test
	void constructor_failedWithoutError_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(1L, "Site A", 3, 2, null));
	}

	@Test
	void constructor_failedWithBlankError_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WebContentPerSiteResult(1L, "Site A", 3, 2, "  "));
	}

}
