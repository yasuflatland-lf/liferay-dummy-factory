package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmailDomainTest {

	@Test
	void ofNullReturnsLiferayCom() {
		assertEquals("liferay.com", EmailDomain.of(null).value());
	}

	@Test
	void ofBlankReturnsLiferayCom() {
		assertEquals("liferay.com", EmailDomain.of("").value());
	}

	@Test
	void ofValidStringPreservesValue() {
		assertEquals("example.com", EmailDomain.of("example.com").value());
	}

	@Test
	void constructorRejectsBlank() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new EmailDomain(""));
	}

	@Test
	void constructorRejectsAtSign() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new EmailDomain("bad@domain.com"));
	}

	@Test
	void constructorRejectsNoDot() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new EmailDomain("nodotdomain"));
	}

	@Test
	void constructorAcceptsValid() {
		new EmailDomain("valid.com");
	}

	@Test
	void toEmailAddressComposesCorrectly() {
		assertEquals(
			"alice@example.com",
			new EmailDomain("example.com").toEmailAddress("alice"));
	}

	@Test
	void recordEqualityWorks() {
		assertEquals(new EmailDomain("a.b"), new EmailDomain("a.b"));
	}

}
