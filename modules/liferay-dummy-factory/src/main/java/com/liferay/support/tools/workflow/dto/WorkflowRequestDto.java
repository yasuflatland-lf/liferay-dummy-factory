package com.liferay.support.tools.workflow.dto;

import java.util.List;
import java.util.Map;

public record WorkflowRequestDto(
	String schemaVersion,
	String workflowId,
	Map<String, Object> input,
	List<WorkflowStepDto> steps) {
}
