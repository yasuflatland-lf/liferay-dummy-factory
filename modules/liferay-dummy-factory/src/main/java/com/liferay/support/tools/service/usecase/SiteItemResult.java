package com.liferay.support.tools.service.usecase;

public record SiteItemResult(
	long groupId,
	String name,
	boolean inheritContent,
	long parentGroupId,
	String publicLayoutSetPrototypeUuid,
	String privateLayoutSetPrototypeUuid) {

	public SiteItemResult {
		if (groupId <= 0) {
			throw new IllegalArgumentException("groupId must be > 0");
		}

		if ((name == null) || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
	}

}
