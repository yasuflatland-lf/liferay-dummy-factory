package com.liferay.support.tools.service;

public record MBReplyBatchSpec(
	BatchSpec batch,
	long threadId,
	String body,
	String format,
	boolean fakerEnable,
	String locale) {

	public MBReplyBatchSpec {
		format = _nullOrEmptyToDefault(format, "html");
		locale = _nullOrEmptyToDefault(locale, "en_US");
		body = (body == null) ? "" : body;
	}

	private static String _nullOrEmptyToDefault(
		String value, String defaultValue) {

		if ((value == null) || value.isEmpty()) {
			return defaultValue;
		}

		return value;
	}

}
