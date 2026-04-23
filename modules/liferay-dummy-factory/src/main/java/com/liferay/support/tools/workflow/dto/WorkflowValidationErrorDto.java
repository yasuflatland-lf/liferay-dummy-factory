package com.liferay.support.tools.workflow.dto;

public record WorkflowValidationErrorDto(
	String code,
	String path,
	String message) {
}
