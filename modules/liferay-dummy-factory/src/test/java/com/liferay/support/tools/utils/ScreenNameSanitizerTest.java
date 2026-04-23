package com.liferay.support.tools.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScreenNameSanitizerTest {

	@Test
	void nullInputFallsBackToUser() {
		assertEquals("user", ScreenNameSanitizer.sanitize(null));
	}

	@Test
	void emptyInputFallsBackToUser() {
		assertEquals("user", ScreenNameSanitizer.sanitize(""));
	}

	@Test
	void apostropheIsStrippedInLowercaseName() {
		assertEquals(
			"krystal.oconner2",
			ScreenNameSanitizer.sanitize("krystal.o'conner2"));
	}

	@Test
	void apostropheIsStrippedPreservingCase() {
		assertEquals("OBrien", ScreenNameSanitizer.sanitize("O'Brien"));
	}

	@Test
	void apostropheIsStrippedInMixedCaseWithDot() {
		assertEquals(
			"dAngelo.smith3",
			ScreenNameSanitizer.sanitize("d'Angelo.smith3"));
	}

	@Test
	void hyphenAndDotAreAllowed() {
		assertEquals(
			"jean-luc.picard",
			ScreenNameSanitizer.sanitize("jean-luc.picard"));
	}

	@Test
	void underscoreIsAllowed() {
		assertEquals(
			"under_score", ScreenNameSanitizer.sanitize("under_score"));
	}

	@Test
	void happyPathIsUnchanged() {
		assertEquals(
			"normal.name1", ScreenNameSanitizer.sanitize("normal.name1"));
	}

	@Test
	void consecutiveDotsAreCollapsedAndEdgesStripped() {
		assertEquals(
			"double.dots",
			ScreenNameSanitizer.sanitize("..double..dots.."));
	}

	@Test
	void allInvalidCharactersFallBackToUser() {
		assertEquals("user", ScreenNameSanitizer.sanitize("!!!"));
	}

	@Test
	void onlySeparatorsFallBackToUser() {
		assertEquals("user", ScreenNameSanitizer.sanitize("..."));
	}

	@Test
	void nonAsciiCharactersAreStripped() {
		assertEquals("tarou1", ScreenNameSanitizer.sanitize("山田.tarou1"));
	}

	@Test
	void emailAtSymbolIsStripped() {
		assertEquals("ab.com", ScreenNameSanitizer.sanitize("a@b.com"));
	}

	@Test
	void whitespaceIsStripped() {
		assertEquals("foobar", ScreenNameSanitizer.sanitize("foo bar"));
	}

	@Test
	void allWhitespaceFallsBackToUser() {
		assertEquals("user", ScreenNameSanitizer.sanitize("  "));
	}

	@Test
	void mixedCaseIsPreserved() {
		assertEquals(
			"MixedCase.Name1",
			ScreenNameSanitizer.sanitize("MixedCase.Name1"));
	}

}
