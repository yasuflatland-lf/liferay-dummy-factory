package com.liferay.support.tools.portlet.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceCommandUtilTest {

	@Test
	void validateCount_positive_passes() {
		assertDoesNotThrow(() -> ResourceCommandUtil.validateCount(1));
	}

	@Test
	void validateCount_zero_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> ResourceCommandUtil.validateCount(0));
	}

	@Test
	void validateCount_negative_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> ResourceCommandUtil.validateCount(-1));
	}

	@Test
	void validatePositiveId_positive_passes() {
		assertDoesNotThrow(
			() -> ResourceCommandUtil.validatePositiveId(1L, "id"));
	}

	@Test
	void validatePositiveId_zero_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> ResourceCommandUtil.validatePositiveId(0L, "id"));
	}

	@Test
	void validatePositiveId_negative_throws() {
		assertThrows(
			IllegalArgumentException.class,
			() -> ResourceCommandUtil.validatePositiveId(-1L, "id"));
	}

}
