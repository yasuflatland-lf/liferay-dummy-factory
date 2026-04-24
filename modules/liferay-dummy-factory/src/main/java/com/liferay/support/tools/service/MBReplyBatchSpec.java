package com.liferay.support.tools.service;

public record MBReplyBatchSpec(
	BatchSpec batch,
	long threadId,
	String body,
	String format,
	boolean fakerEnable,
	String locale) {

	public MBReplyBatchSpec {
		format = ((format == null) || format.isEmpty()) ? "html" : format;
		locale = ((locale == null) || locale.isEmpty()) ? "en_US" : locale;
		body = (body == null) ? "" : body;
	}

}
