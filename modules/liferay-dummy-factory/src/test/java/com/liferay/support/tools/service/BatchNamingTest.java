package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BatchNamingTest {

	@Test
	void singleItemUsesBaseNameDirectly() {
		assertEquals("Site", BatchNaming.resolve("Site", 1, 0));
	}

	@Test
	void multipleItemsFirstIndex() {
		assertEquals("Site1", BatchNaming.resolve("Site", 3, 0));
	}

	@Test
	void multipleItemsLastIndex() {
		assertEquals("Site3", BatchNaming.resolve("Site", 3, 2));
	}

	@Test
	void differentBaseNameLastItem() {
		assertEquals("Role5", BatchNaming.resolve("Role", 5, 4));
	}

	@Test
	void separatorVariant() {
		assertEquals("Org 1", BatchNaming.resolve("Org", 3, 0, " "));
	}

	@Test
	void separatorVariantWithSingleCount() {
		assertEquals("Org", BatchNaming.resolve("Org", 1, 0, " "));
	}

}
