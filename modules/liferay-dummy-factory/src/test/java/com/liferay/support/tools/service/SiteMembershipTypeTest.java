package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.liferay.portal.kernel.model.GroupConstants;

import org.junit.jupiter.api.Test;

class SiteMembershipTypeTest {

	@Test
	void fromStringUpperCase() {
		assertEquals(
			SiteMembershipType.OPEN,
			SiteMembershipType.fromString("OPEN"));
	}

	@Test
	void fromStringLowerCase() {
		assertEquals(
			SiteMembershipType.OPEN,
			SiteMembershipType.fromString("open"));
	}

	@Test
	void fromStringRestricted() {
		assertEquals(
			SiteMembershipType.RESTRICTED,
			SiteMembershipType.fromString("restricted"));
	}

	@Test
	void fromStringUnknownThrowsException() {
		assertThrows(
			IllegalArgumentException.class,
			() -> SiteMembershipType.fromString("unknown"));
	}

	@Test
	void toLiferayConstant() {
		assertEquals(
			GroupConstants.TYPE_SITE_OPEN,
			SiteMembershipType.OPEN.toLiferayConstant());
		assertEquals(
			GroupConstants.TYPE_SITE_RESTRICTED,
			SiteMembershipType.RESTRICTED.toLiferayConstant());
		assertEquals(
			GroupConstants.TYPE_SITE_PRIVATE,
			SiteMembershipType.PRIVATE.toLiferayConstant());
	}

}
