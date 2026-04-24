package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MBReplyBatchSpecTest {

	@Test
	void allCustomValuesPreserved() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(5, "Reply"),
			999L,
			"Reply body.",
			"bbcode",
			true,
			"ja_JP",
			AssetTagNames.of("replies"));

		assertEquals(5, spec.batch().count());
		assertEquals("Reply", spec.batch().baseName());
		assertEquals(999L, spec.threadId());
		assertEquals("Reply body.", spec.body());
		assertEquals("bbcode", spec.format());
		assertTrue(spec.fakerEnable());
		assertEquals("ja_JP", spec.locale());
		assertEquals(1, spec.tags().names().size());
	}

	@Test
	void nullFormatDefaultsToHtml() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(1, "R"), 1L, "body", null, false, "en_US",
			AssetTagNames.EMPTY);

		assertEquals("html", spec.format());
	}

	@Test
	void emptyFormatDefaultsToHtml() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(1, "R"), 1L, "body", "", false, "en_US",
			AssetTagNames.EMPTY);

		assertEquals("html", spec.format());
	}

	@Test
	void nullLocaleDefaultsToEnUs() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(1, "R"), 1L, "body", "html", false, null,
			AssetTagNames.EMPTY);

		assertEquals("en_US", spec.locale());
	}

	@Test
	void emptyLocaleDefaultsToEnUs() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(1, "R"), 1L, "body", "html", false, "",
			AssetTagNames.EMPTY);

		assertEquals("en_US", spec.locale());
	}

	@Test
	void nullBodyNormalisedToEmpty() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(1, "R"), 1L, null, "html", false, "en_US",
			AssetTagNames.EMPTY);

		assertEquals("", spec.body());
	}

	@Test
	void nullTagsDefaultsToEmpty() {
		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(1, "R"), 1L, "body", "html", false, "en_US", null);

		assertSame(AssetTagNames.EMPTY, spec.tags());
	}

	@Test
	void composedBatchSpecValidation() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBReplyBatchSpec(
				new BatchSpec(0, "Bad"), 1L, "body", "html", false, "en_US",
				AssetTagNames.EMPTY));

		assertThrows(
			IllegalArgumentException.class,
			() -> new MBReplyBatchSpec(
				new BatchSpec(1, null), 1L, "body", "html", false, "en_US",
				AssetTagNames.EMPTY));
	}

}
