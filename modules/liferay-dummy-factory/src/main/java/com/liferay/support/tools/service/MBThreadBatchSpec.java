package com.liferay.support.tools.service;

public record MBThreadBatchSpec(
	BatchSpec batch,
	long groupId,
	long categoryId,
	String body,
	String format,
	AssetTagNames tags) {

	public MBThreadBatchSpec {
		body = (body == null) ? "This is a test message." : body;
		format = (format == null || format.isEmpty()) ? "html" : format;
		tags = (tags == null) ? AssetTagNames.EMPTY : tags;
	}

}
