package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BatchSpecTest {

	@Test
	void normalCreation() {
		BatchSpec spec = new BatchSpec(5, "TestSite");

		assertEquals(5, spec.count());
		assertEquals("TestSite", spec.baseName());
	}

	@Test
	void boundaryValues() {
		BatchSpec spec = new BatchSpec(1, "a");

		assertEquals(1, spec.count());
		assertEquals("a", spec.baseName());
	}

	@Test
	void zeroCountThrows() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class, () -> new BatchSpec(0, "Test"));

		assertEquals("count must be greater than 0", exception.getMessage());
	}

	@Test
	void negativeCountThrows() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class, () -> new BatchSpec(-1, "Test"));

		assertEquals("count must be greater than 0", exception.getMessage());
	}

	@Test
	void nullBaseNameThrows() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class, () -> new BatchSpec(1, null));

		assertEquals("baseName is required", exception.getMessage());
	}

	@Test
	void emptyBaseNameThrows() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class, () -> new BatchSpec(1, ""));

		assertEquals("baseName is required", exception.getMessage());
	}

}
