package com.liferay.support.tools.workflow.adapter.taxonomy.dto;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.adapter.WorkflowInputValidator;

import java.util.Objects;

public record CategoryCreateRequest(
	long userId, long groupId, long vocabularyId, BatchSpec batch) {

	public CategoryCreateRequest {
		userId = WorkflowInputValidator.requirePositiveId(userId, "userId");
		groupId = WorkflowInputValidator.requirePositiveId(groupId, "groupId");
		vocabularyId = WorkflowInputValidator.requirePositiveId(
			vocabularyId, "vocabularyId");
		batch = Objects.requireNonNull(batch, "batch is required");
	}

}
