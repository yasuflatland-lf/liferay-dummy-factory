package com.liferay.support.tools.workflow.dto;

import java.util.List;

public record WorkflowStepDto(
	String id,
	String operation,
	String idempotencyKey,
	List<WorkflowParameterDto> params,
	WorkflowOnErrorDto onError) {
}
