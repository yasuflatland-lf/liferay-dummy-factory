package com.liferay.support.tools.service;

public record WebContentBatchSpec(
	BatchSpec batch,
	long[] groupIds,
	long folderId,
	String[] locales,
	boolean neverExpire,
	boolean neverReview,
	int createContentsType,
	String baseArticle,
	int titleWords,
	int totalParagraphs,
	int randomAmount,
	String linkLists,
	long ddmStructureId,
	long ddmTemplateId,
	AssetTagNames tags) {

	public WebContentBatchSpec {
		groupIds = (groupIds == null) ? new long[0] : groupIds;
		locales = (locales == null) ? new String[0] : locales;
		baseArticle = (baseArticle == null) ? "" : baseArticle;
		linkLists = (linkLists == null) ? "" : linkLists;
		tags = (tags == null) ? AssetTagNames.EMPTY : tags;
	}

}
