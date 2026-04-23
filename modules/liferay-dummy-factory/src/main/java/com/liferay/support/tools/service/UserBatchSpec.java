package com.liferay.support.tools.service;

import java.util.Objects;

public record UserBatchSpec(
	BatchSpec batch,
	EmailDomain emailDomain,
	String password,
	boolean male,
	String jobTitle,
	long[] organizationIds,
	long[] roleIds,
	long[] userGroupIds,
	long[] siteRoleIds,
	long[] orgRoleIds,
	boolean fakerEnable,
	String locale,
	boolean generatePersonalSiteLayouts,
	long publicLayoutSetPrototypeId,
	long privateLayoutSetPrototypeId,
	long[] groupIds) {

	public UserBatchSpec {
		Objects.requireNonNull(emailDomain, "emailDomain is required");
		password = _nullOrEmptyToDefault(password, "test");
		locale = _nullOrEmptyToDefault(locale, "en_US");
		jobTitle = (jobTitle == null) ? "" : jobTitle;
		organizationIds = _nullToEmpty(organizationIds);
		roleIds = _nullToEmpty(roleIds);
		userGroupIds = _nullToEmpty(userGroupIds);
		siteRoleIds = _nullToEmpty(siteRoleIds);
		orgRoleIds = _nullToEmpty(orgRoleIds);
		groupIds = _nullToEmpty(groupIds);
	}

	private static long[] _nullToEmpty(long[] array) {
		return (array == null) ? new long[0] : array;
	}

	private static String _nullOrEmptyToDefault(
		String value, String defaultValue) {

		if ((value == null) || value.isEmpty()) {
			return defaultValue;
		}

		return value;
	}

}
