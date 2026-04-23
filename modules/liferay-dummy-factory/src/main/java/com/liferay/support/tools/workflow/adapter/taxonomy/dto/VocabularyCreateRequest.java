package com.liferay.support.tools.workflow.adapter.taxonomy.dto;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.adapter.WorkflowInputValidator;

import java.util.Objects;

public record VocabularyCreateRequest(
	long userId, long groupId, BatchSpec batch) {

	public VocabularyCreateRequest {
		userId = WorkflowInputValidator.requirePositiveId(userId, "userId");
		groupId = WorkflowInputValidator.requirePositiveId(groupId, "groupId");
		batch = Objects.requireNonNull(batch, "batch is required");
	}

}
