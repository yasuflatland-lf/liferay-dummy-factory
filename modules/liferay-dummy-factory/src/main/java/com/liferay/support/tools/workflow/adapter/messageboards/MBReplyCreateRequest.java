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

		if ((body == null) || body.isBlank()) {
			throw new IllegalArgumentException("body is required");
		}

		format = WorkflowInputValidator.normalizeText(format, "html");
		locale = WorkflowInputValidator.normalizeText(locale, "en_US");
	}

}
