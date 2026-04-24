package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.support.tools.workflow.adapter.WorkflowInputValidator;

public record MBReplyCreateRequest(
	long userId, long threadId, int count, String body, String format,
	boolean fakerEnable, String locale) {

	public MBReplyCreateRequest {
		userId = WorkflowInputValidator.requirePositiveId(userId, "userId");
		threadId = WorkflowInputValidator.requirePositiveId(
			threadId, "threadId");
		count = WorkflowInputValidator.requireCount(count);
		body = _requireText(body, "body");
		format = WorkflowInputValidator.normalizeText(format, "html");
		locale = WorkflowInputValidator.normalizeText(locale, "en_US");
	}

	private static String _requireText(String value, String fieldName) {
		if ((value == null) || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}

		return value;
	}

}
