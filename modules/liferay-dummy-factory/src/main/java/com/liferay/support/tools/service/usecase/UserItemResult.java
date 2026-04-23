package com.liferay.support.tools.service.usecase;

public record UserItemResult(
	String emailAddress,
	String screenName,
	long userId,
	long groupId,
	String publicLayoutSetPrototypeUuid,
	String privateLayoutSetPrototypeUuid) {

	public UserItemResult {
		if ((emailAddress == null) || emailAddress.isBlank()) {
			throw new IllegalArgumentException("emailAddress is required");
		}

		if ((screenName == null) || screenName.isBlank()) {
			throw new IllegalArgumentException("screenName is required");
		}

		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be > 0");
		}

		if ((groupId == 0L) &&
			((publicLayoutSetPrototypeUuid != null) ||
				(privateLayoutSetPrototypeUuid != null))) {

			throw new IllegalArgumentException(
				"layoutSetPrototypeUuid requires groupId > 0");
		}
	}

}
