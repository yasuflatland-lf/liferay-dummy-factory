package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DocumentBatchSpecTest {

	@Test
	void allCustomValuesPreserved() {
		String[] files = {"file1.pdf", "file2.png"};

		DocumentBatchSpec spec = new DocumentBatchSpec(
			new BatchSpec(4, "Doc"),
			10L,
			20L,
			"A description.",
			files,
			AssetTagNames.of("docs,archive"));

		assertEquals(4, spec.batch().count());
		assertEquals("Doc", spec.batch().baseName());
		assertEquals(10L, spec.groupId());
		assertEquals(20L, spec.folderId());
		assertEquals("A description.", spec.description());
		assertArrayEquals(files, spec.uploadedFiles());
		assertEquals(2, spec.tags().names().size());
	}

	@Test
	void nullDescriptionNormalisedToEmpty() {
		DocumentBatchSpec spec = new DocumentBatchSpec(
			new BatchSpec(1, "D"), 1L, 0L, null, new String[0],
			AssetTagNames.EMPTY);

		assertEquals("", spec.description());
	}

	@Test
	void nullUploadedFilesNormalisedToEmptyArray() {
		DocumentBatchSpec spec = new DocumentBatchSpec(
			new BatchSpec(1, "D"), 1L, 0L, "", null, AssetTagNames.EMPTY);

		assertEquals(0, spec.uploadedFiles().length);
	}

	@Test
	void nullTagsDefaultsToEmpty() {
		DocumentBatchSpec spec = new DocumentBatchSpec(
			new BatchSpec(1, "D"), 1L, 0L, "", new String[0], null);

		assertSame(AssetTagNames.EMPTY, spec.tags());
	}

	@Test
	void composedBatchSpecValidation() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new DocumentBatchSpec(
				new BatchSpec(0, "Bad"), 1L, 0L, "", new String[0],
				AssetTagNames.EMPTY));

		assertThrows(
			IllegalArgumentException.class,
			() -> new DocumentBatchSpec(
				new BatchSpec(1, ""), 1L, 0L, "", new String[0],
				AssetTagNames.EMPTY));
	}

}
