package com.liferay.support.tools.workflow;

public record WorkflowError(String code, String message) {

	public WorkflowError {
		if ((code == null) || code.isBlank()) {
			throw new IllegalArgumentException("code is required");
		}

		if ((message == null) || message.isBlank()) {
			throw new IllegalArgumentException("message is required");
		}
	}

}
