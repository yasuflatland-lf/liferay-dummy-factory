package com.liferay.support.tools.workflow;

import java.util.List;

public record WorkflowExecutionResult(
	String workflowId,
	WorkflowExecutionStatus status,
	List<WorkflowStepExecutionResult> steps,
	String failedStepId) {

	public WorkflowExecutionResult {
		if (status == null) {
			throw new IllegalArgumentException("status is required");
		}

		steps = List.copyOf(steps);
	}

}
