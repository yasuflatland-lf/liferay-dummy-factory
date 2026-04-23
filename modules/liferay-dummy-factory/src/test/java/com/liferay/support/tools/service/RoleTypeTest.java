package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.liferay.portal.kernel.model.role.RoleConstants;

import org.junit.jupiter.api.Test;

class RoleTypeTest {

	@Test
	void fromStringUpperCase() {
		assertEquals(RoleType.REGULAR, RoleType.fromString("REGULAR"));
	}

	@Test
	void fromStringLowerCase() {
		assertEquals(RoleType.SITE, RoleType.fromString("site"));
	}

	@Test
	void fromStringInvalidThrowsException() {
		assertThrows(
			IllegalArgumentException.class,
			() -> RoleType.fromString("invalid"));
	}

	@Test
	void toLiferayConstant() {
		assertEquals(
			RoleConstants.TYPE_REGULAR,
			RoleType.REGULAR.toLiferayConstant());
		assertEquals(
			RoleConstants.TYPE_SITE,
			RoleType.SITE.toLiferayConstant());
		assertEquals(
			RoleConstants.TYPE_ORGANIZATION,
			RoleType.ORGANIZATION.toLiferayConstant());
	}

}
