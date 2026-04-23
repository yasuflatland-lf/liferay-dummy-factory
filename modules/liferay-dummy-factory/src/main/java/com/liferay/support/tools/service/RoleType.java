package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.role.RoleConstants;

public enum RoleType {

	REGULAR(RoleConstants.TYPE_REGULAR),
	SITE(RoleConstants.TYPE_SITE),
	ORGANIZATION(RoleConstants.TYPE_ORGANIZATION),
	PROVIDER(RoleConstants.TYPE_PROVIDER),
	DEPOT(RoleConstants.TYPE_DEPOT),
	ACCOUNT(RoleConstants.TYPE_ACCOUNT),
	PUBLICATIONS(RoleConstants.TYPE_PUBLICATIONS);

	public static RoleType fromString(String value) {
		for (RoleType roleType : values()) {
			if (roleType.name().equalsIgnoreCase(value)) {
				return roleType;
			}
		}

		throw new IllegalArgumentException("Unknown role type: " + value);
	}

	public int toLiferayConstant() {
		return _liferayConstant;
	}

	private RoleType(int liferayConstant) {
		_liferayConstant = liferayConstant;
	}

	private final int _liferayConstant;

}
