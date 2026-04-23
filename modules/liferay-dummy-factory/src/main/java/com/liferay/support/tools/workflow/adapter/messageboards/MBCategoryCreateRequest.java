package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.adapter.WorkflowInputValidator;

import java.util.Objects;

public record MBCategoryCreateRequest(
	long userId, long groupId, BatchSpec batch, String description) {

	public MBCategoryCreateRequest {
		userId = WorkflowInputValidator.requirePositiveId(userId, "userId");
		groupId = WorkflowInputValidator.requirePositiveId(groupId, "groupId");
		batch = Objects.requireNonNull(batch, "batch is required");
		description = _requireText(description, "description");
	}

	private static String _requireText(String value, String fieldName) {
		if ((value == null) || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}

		return value;
	}

}
