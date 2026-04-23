package com.liferay.support.tools.workflow.adapter.core.dto;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.EmailDomain;
import com.liferay.support.tools.service.UserBatchSpec;

import java.util.Objects;

public record UserCreateRequest(
	BatchSpec batch, String emailDomain, boolean fakerEnable,
	boolean generatePersonalSiteLayouts, long[] groupIds, String jobTitle,
	String locale, boolean male, long[] orgRoleIds, long[] organizationIds,
	String password, long privateLayoutSetPrototypeId,
	long publicLayoutSetPrototypeId, long[] roleIds, long[] siteRoleIds,
	long[] userGroupIds) {

	public UserCreateRequest {
		Objects.requireNonNull(batch, "batch is required");
		groupIds = _nullToEmpty(groupIds);
		orgRoleIds = _nullToEmpty(orgRoleIds);
		organizationIds = _nullToEmpty(organizationIds);
		roleIds = _nullToEmpty(roleIds);
		siteRoleIds = _nullToEmpty(siteRoleIds);
		userGroupIds = _nullToEmpty(userGroupIds);
	}

	public UserBatchSpec toUserBatchSpec() {
		return new UserBatchSpec(
			batch, EmailDomain.of(emailDomain), password, male, jobTitle, organizationIds,
			roleIds, userGroupIds, siteRoleIds, orgRoleIds, fakerEnable,
			locale, generatePersonalSiteLayouts,
			publicLayoutSetPrototypeId, privateLayoutSetPrototypeId, groupIds);
	}

	private static long[] _nullToEmpty(long[] array) {
		return (array == null) ? new long[0] : array;
	}

}
