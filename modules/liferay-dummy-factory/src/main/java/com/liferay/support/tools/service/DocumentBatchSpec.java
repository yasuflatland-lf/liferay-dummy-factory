package com.liferay.support.tools.service;

public record DocumentBatchSpec(
	BatchSpec batch,
	long groupId,
	long folderId,
	String description,
	String[] uploadedFiles,
	AssetTagNames tags) {

	public DocumentBatchSpec {
		description = (description == null) ? "" : description;
		uploadedFiles = (uploadedFiles == null) ? new String[0] : uploadedFiles.clone();
		tags = (tags == null) ? AssetTagNames.EMPTY : tags;
	}

}
