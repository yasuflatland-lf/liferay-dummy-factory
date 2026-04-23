package com.liferay.support.tools.workflow.dto;

import com.liferay.support.tools.workflow.WorkflowPlan;

import java.util.List;

public record WorkflowPlanResponseDto(
	WorkflowPlan plan,
	List<WorkflowValidationErrorDto> errors) {
}
