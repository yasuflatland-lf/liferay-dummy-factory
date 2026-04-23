package com.liferay.support.tools.workflow.dto;

import com.liferay.support.tools.workflow.WorkflowExecutionResult;

import java.util.List;

public record WorkflowExecuteResponseDto(
	WorkflowExecutionResult execution,
	List<WorkflowValidationErrorDto> errors) {
}
