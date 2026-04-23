package com.liferay.support.tools.service;

public record EmailDomain(String value) {

	public EmailDomain {
		if ((value == null) || value.isBlank()) {
			throw new IllegalArgumentException(
				"emailDomain must not be blank");
		}

		if (value.contains("@")) {
			throw new IllegalArgumentException(
				"emailDomain must not contain '@'");
		}

		if (!value.contains(".")) {
			throw new IllegalArgumentException(
				"emailDomain must contain at least one '.'");
		}
	}

	public static EmailDomain of(String raw) {
		if ((raw == null) || raw.isBlank()) {
			return new EmailDomain("liferay.com");
		}

		return new EmailDomain(raw);
	}

	public String toEmailAddress(String screenName) {
		return screenName + "@" + value;
	}

}
