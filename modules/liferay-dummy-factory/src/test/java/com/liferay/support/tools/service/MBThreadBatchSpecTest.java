package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MBThreadBatchSpecTest {

	@Test
	void allCustomValuesPreserved() {
		MBThreadBatchSpec spec = new MBThreadBatchSpec(
			new BatchSpec(3, "Thread"),
			100L,
			200L,
			"Custom body text.",
			"bbcode",
			AssetTagNames.of("alpha,beta"));

		assertEquals(3, spec.batch().count());
		assertEquals("Thread", spec.batch().baseName());
		assertEquals(100L, spec.groupId());
		assertEquals(200L, spec.categoryId());
		assertEquals("Custom body text.", spec.body());
		assertEquals("bbcode", spec.format());
		assertEquals(2, spec.tags().names().size());
	}

	@Test
	void nullBodyDefaultsToTestMessage() {
		MBThreadBatchSpec spec = new MBThreadBatchSpec(
			new BatchSpec(1, "T"), 1L, 0L, null, "html", AssetTagNames.EMPTY);

		assertEquals("This is a test message.", spec.body());
	}

	@Test
	void nullFormatDefaultsToHtml() {
		MBThreadBatchSpec spec = new MBThreadBatchSpec(
			new BatchSpec(1, "T"), 1L, 0L, "body", null, AssetTagNames.EMPTY);

		assertEquals("html", spec.format());
	}

	@Test
	void emptyFormatDefaultsToHtml() {
		MBThreadBatchSpec spec = new MBThreadBatchSpec(
			new BatchSpec(1, "T"), 1L, 0L, "body", "", AssetTagNames.EMPTY);

		assertEquals("html", spec.format());
	}

	@Test
	void nullTagsDefaultsToEmpty() {
		MBThreadBatchSpec spec = new MBThreadBatchSpec(
			new BatchSpec(1, "T"), 1L, 0L, "body", "html", null);

		assertSame(AssetTagNames.EMPTY, spec.tags());
	}

	@Test
	void composedBatchSpecValidation() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBThreadBatchSpec(
				new BatchSpec(0, "Bad"), 1L, 0L, "body", "html",
				AssetTagNames.EMPTY));

		assertThrows(
			IllegalArgumentException.class,
			() -> new MBThreadBatchSpec(
				new BatchSpec(1, null), 1L, 0L, "body", "html",
				AssetTagNames.EMPTY));
	}

}
