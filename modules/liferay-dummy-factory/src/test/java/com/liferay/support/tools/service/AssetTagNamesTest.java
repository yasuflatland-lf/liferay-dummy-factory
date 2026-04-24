package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AssetTagNamesTest {

	@Test
	void of_null_returns_empty() {
		assertSame(AssetTagNames.EMPTY, AssetTagNames.of(null));
	}

	@Test
	void of_blank_returns_empty() {
		assertSame(AssetTagNames.EMPTY, AssetTagNames.of("   "));
	}

	@Test
	void of_single_token() {
		assertEquals(List.of("foo"), AssetTagNames.of("foo").names());
	}

	@Test
	void of_comma_separated() {
		assertEquals(List.of("foo", "bar"), AssetTagNames.of("foo,bar").names());
	}

	@Test
	void of_newline_separated() {
		assertEquals(List.of("foo", "bar"), AssetTagNames.of("foo\nbar").names());
	}

	@Test
	void of_crlf_separated() {
		assertEquals(
			List.of("foo", "bar"), AssetTagNames.of("foo\r\nbar").names());
	}

	@Test
	void of_trims_whitespace_around_tokens() {
		assertEquals(List.of("a", "b"), AssetTagNames.of(" a , b ").names());
	}

	@Test
	void of_lowercases_tokens() {
		assertEquals(
			List.of("foo", "bar", "baz"),
			AssetTagNames.of("Foo,BAR,baZ").names());
	}

	@Test
	void of_deduplicates_case_insensitive() {
		assertEquals(List.of("foo"), AssetTagNames.of("foo,Foo,FOO").names());
	}

	@Test
	void of_preserves_internal_whitespace() {
		assertEquals(List.of("foo bar"), AssetTagNames.of("foo bar").names());
	}

	@Test
	void of_preserves_insertion_order() {
		assertEquals(
			List.of("zeta", "alpha"), AssetTagNames.of("zeta,alpha").names());
	}

	@Test
	void isEmpty_returns_true_for_EMPTY() {
		assertTrue(AssetTagNames.EMPTY.isEmpty());
	}

	@Test
	void isEmpty_returns_false_for_non_empty() {
		assertFalse(AssetTagNames.of("foo").isEmpty());
	}

	@Test
	void toArray_is_defensive_copy() {
		AssetTagNames assetTagNames = AssetTagNames.of("foo");

		String[] array = assetTagNames.toArray();

		array[0] = "mutated";

		assertEquals(List.of("foo"), assetTagNames.names());
	}

}
