package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.GroupConstants;

public enum SiteMembershipType {

	OPEN(GroupConstants.TYPE_SITE_OPEN),
	RESTRICTED(GroupConstants.TYPE_SITE_RESTRICTED),
	PRIVATE(GroupConstants.TYPE_SITE_PRIVATE);

	public static SiteMembershipType fromString(String value) {
		for (SiteMembershipType type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}

		throw new IllegalArgumentException("Unknown membership type: " + value);
	}

	public int toLiferayConstant() {
		return _liferayConstant;
	}

	private SiteMembershipType(int liferayConstant) {
		_liferayConstant = liferayConstant;
	}

	private final int _liferayConstant;

}
