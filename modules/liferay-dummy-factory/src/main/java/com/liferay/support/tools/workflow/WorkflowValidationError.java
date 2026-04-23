package com.liferay.support.tools.workflow;

public record WorkflowValidationError(
	String code,
	String path,
	String message) {

	public WorkflowValidationError {
		if ((code == null) || code.isBlank()) {
			throw new IllegalArgumentException("code is required");
		}

		if ((path == null) || path.isBlank()) {
			throw new IllegalArgumentException("path is required");
		}

		if ((message == null) || message.isBlank()) {
			throw new IllegalArgumentException("message is required");
		}
	}

}
