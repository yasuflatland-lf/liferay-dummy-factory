package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.liferay.support.tools.service.AssetTagNames;

class WebContentBatchSpecTest {

	@Test
	void allFieldsPreserved() {
		WebContentBatchSpec spec = _builder().build();

		assertEquals(5, spec.batch().count());
		assertEquals("Article", spec.batch().baseName());
		assertArrayEquals(new long[] {123L}, spec.groupIds());
		assertEquals(42L, spec.folderId());
		assertArrayEquals(new String[] {"en_US"}, spec.locales());
		assertTrue(spec.neverExpire());
		assertFalse(spec.neverReview());
		assertEquals(0, spec.createContentsType());
		assertEquals("<p>hello</p>", spec.baseArticle());
		assertEquals(7, spec.titleWords());
		assertEquals(4, spec.totalParagraphs());
		assertEquals(5, spec.randomAmount());
		assertEquals("http://example.com/img.png", spec.linkLists());
		assertEquals(100L, spec.ddmStructureId());
		assertEquals(200L, spec.ddmTemplateId());
	}

	@Test
	void nullGroupIdsNormalisedToEmpty() {
		WebContentBatchSpec spec = _builder().groupIds(null).build();

		assertArrayEquals(new long[0], spec.groupIds());
	}

	@Test
	void nullLocalesNormalisedToEmpty() {
		WebContentBatchSpec spec = _builder().locales(null).build();

		assertArrayEquals(new String[0], spec.locales());
	}

	@Test
	void nullBaseArticleNormalisedToEmpty() {
		WebContentBatchSpec spec = _builder().baseArticle(null).build();

		assertEquals("", spec.baseArticle());
	}

	@Test
	void nullLinkListsNormalisedToEmpty() {
		WebContentBatchSpec spec = _builder().linkLists(null).build();

		assertEquals("", spec.linkLists());
	}

	@Test
	void customValuesPreserved() {
		WebContentBatchSpec spec = new WebContentBatchSpec(
			new BatchSpec(10, "WC"),
			new long[] {1L, 2L, 3L},
			99L,
			new String[] {"ja_JP", "en_US"},
			false,
			true,
			2,
			"custom-article",
			10,
			8,
			6,
			"http://link1\nhttp://link2",
			500L,
			600L,
			AssetTagNames.EMPTY);

		assertEquals(10, spec.batch().count());
		assertEquals("WC", spec.batch().baseName());
		assertArrayEquals(new long[] {1L, 2L, 3L}, spec.groupIds());
		assertEquals(99L, spec.folderId());
		assertArrayEquals(
			new String[] {"ja_JP", "en_US"}, spec.locales());
		assertFalse(spec.neverExpire());
		assertTrue(spec.neverReview());
		assertEquals(2, spec.createContentsType());
		assertEquals("custom-article", spec.baseArticle());
		assertEquals(10, spec.titleWords());
		assertEquals(8, spec.totalParagraphs());
		assertEquals(6, spec.randomAmount());
		assertEquals("http://link1\nhttp://link2", spec.linkLists());
		assertEquals(500L, spec.ddmStructureId());
		assertEquals(600L, spec.ddmTemplateId());
	}

	@Test
	void composedBatchSpecValidation() {
		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(0, "Bad")).build());

		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(1, null)).build());

		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(1, "")).build());
	}

	@Test
	void defaultsForDummyMode() {
		WebContentBatchSpec spec = new WebContentBatchSpec(
			new BatchSpec(1, "Dummy"),
			new long[] {10L},
			0L,
			null,
			true,
			true,
			1,
			null,
			5,
			3,
			3,
			null,
			0L,
			0L,
			AssetTagNames.EMPTY);

		assertEquals(1, spec.createContentsType());
		assertArrayEquals(new String[0], spec.locales());
		assertEquals("", spec.baseArticle());
		assertEquals("", spec.linkLists());
	}

	@Test
	void defaultsForStructureTemplateMode() {
		WebContentBatchSpec spec = new WebContentBatchSpec(
			new BatchSpec(1, "Struct"),
			new long[] {20L},
			0L,
			new String[] {"en_US"},
			true,
			true,
			2,
			null,
			0,
			0,
			0,
			null,
			300L,
			400L,
			AssetTagNames.EMPTY);

		assertEquals(2, spec.createContentsType());
		assertEquals(300L, spec.ddmStructureId());
		assertEquals(400L, spec.ddmTemplateId());
		assertEquals("", spec.baseArticle());
		assertEquals("", spec.linkLists());
	}

	@Test
	void tags_null_defaults_to_empty() {
		WebContentBatchSpec spec = new WebContentBatchSpec(
			new BatchSpec(1, "WC"),
			new long[] {1L},
			0L,
			new String[] {"en_US"},
			true,
			true,
			0,
			"",
			5,
			3,
			3,
			"",
			0L,
			0L,
			null);

		assertSame(AssetTagNames.EMPTY, spec.tags());
	}

	private static _Builder _builder() {
		return new _Builder();
	}

	private static class _Builder {

		_Builder baseArticle(String baseArticle) {
			_baseArticle = baseArticle;

			return this;
		}

		_Builder batch(BatchSpec batch) {
			_batch = batch;

			return this;
		}

		WebContentBatchSpec build() {
			return new WebContentBatchSpec(
				_batch, _groupIds, _folderId, _locales, _neverExpire,
				_neverReview, _createContentsType, _baseArticle,
				_titleWords, _totalParagraphs, _randomAmount, _linkLists,
				_ddmStructureId, _ddmTemplateId, AssetTagNames.EMPTY);
		}

		_Builder groupIds(long[] groupIds) {
			_groupIds = groupIds;

			return this;
		}

		_Builder linkLists(String linkLists) {
			_linkLists = linkLists;

			return this;
		}

		_Builder locales(String[] locales) {
			_locales = locales;

			return this;
		}

		private String _baseArticle = "<p>hello</p>";
		private BatchSpec _batch = new BatchSpec(5, "Article");
		private int _createContentsType = 0;
		private long _ddmStructureId = 100L;
		private long _ddmTemplateId = 200L;
		private long _folderId = 42L;
		private long[] _groupIds = {123L};
		private String _linkLists = "http://example.com/img.png";
		private String[] _locales = {"en_US"};
		private boolean _neverExpire = true;
		private boolean _neverReview = false;
		private int _randomAmount = 5;
		private int _titleWords = 7;
		private int _totalParagraphs = 4;

	}

}
