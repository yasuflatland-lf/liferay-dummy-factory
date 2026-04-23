package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlogsBatchSpecTest {

	@Test
	void customValuesArePreserved() {
		String[] trackbackURLs = {"http://a.com", "http://b.com"};

		BlogsBatchSpec spec = _builder()
			.batch(new BatchSpec(10, "Blog"))
			.groupId(999L)
			.content("<p>custom</p>")
			.subtitle("My Subtitle")
			.description("My Description")
			.allowPingbacks(false)
			.allowTrackbacks(true)
			.trackbackURLs(trackbackURLs)
			.build();

		assertEquals(10, spec.batch().count());
		assertEquals("Blog", spec.batch().baseName());
		assertEquals(999L, spec.groupId());
		assertEquals("<p>custom</p>", spec.content());
		assertEquals("My Subtitle", spec.subtitle());
		assertEquals("My Description", spec.description());
		assertFalse(spec.allowPingbacks());
		assertTrue(spec.allowTrackbacks());
		assertArrayEquals(trackbackURLs, spec.trackbackURLs());
	}

	@Test
	void nullContentDefaultsToEmptyString() {
		BlogsBatchSpec spec = _builder().content(null).build();

		assertEquals("", spec.content());
	}

	@Test
	void nullSubtitleDefaultsToEmptyString() {
		BlogsBatchSpec spec = _builder().subtitle(null).build();

		assertEquals("", spec.subtitle());
	}

	@Test
	void nullDescriptionDefaultsToEmptyString() {
		BlogsBatchSpec spec = _builder().description(null).build();

		assertEquals("", spec.description());
	}

	@Test
	void nullTrackbackURLsDefaultsToEmptyArray() {
		BlogsBatchSpec spec = _builder().trackbackURLs(null).build();

		assertArrayEquals(new String[0], spec.trackbackURLs());
	}

	@Test
	void defaultAllowPingbacksIsTrue() {
		BlogsBatchSpec spec = _builder().build();

		assertTrue(spec.allowPingbacks());
	}

	@Test
	void defaultAllowTrackbacksIsFalse() {
		BlogsBatchSpec spec = _builder().build();

		assertFalse(spec.allowTrackbacks());
	}

	@Test
	void composedBatchSpecRejectsZeroCount() {
		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(0, "Test")).build());
	}

	@Test
	void composedBatchSpecRejectsEmptyBaseName() {
		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(1, "")).build());
	}

	private static _Builder _builder() {
		return new _Builder();
	}

	private static class _Builder {

		_Builder allowPingbacks(boolean allowPingbacks) {
			_allowPingbacks = allowPingbacks;

			return this;
		}

		_Builder allowTrackbacks(boolean allowTrackbacks) {
			_allowTrackbacks = allowTrackbacks;

			return this;
		}

		_Builder batch(BatchSpec batch) {
			_batch = batch;

			return this;
		}

		BlogsBatchSpec build() {
			return new BlogsBatchSpec(
				_batch, _groupId, _content, _subtitle, _description,
				_allowPingbacks, _allowTrackbacks, _trackbackURLs);
		}

		_Builder content(String content) {
			_content = content;

			return this;
		}

		_Builder description(String description) {
			_description = description;

			return this;
		}

		_Builder groupId(long groupId) {
			_groupId = groupId;

			return this;
		}

		_Builder subtitle(String subtitle) {
			_subtitle = subtitle;

			return this;
		}

		_Builder trackbackURLs(String[] trackbackURLs) {
			_trackbackURLs = trackbackURLs;

			return this;
		}

		private boolean _allowPingbacks = true;
		private boolean _allowTrackbacks = false;
		private BatchSpec _batch = new BatchSpec(1, "test");
		private String _content = "<p>body</p>";
		private String _description = "desc";
		private long _groupId = 20123L;
		private String _subtitle = "sub";
		private String[] _trackbackURLs = new String[0];

	}

}
