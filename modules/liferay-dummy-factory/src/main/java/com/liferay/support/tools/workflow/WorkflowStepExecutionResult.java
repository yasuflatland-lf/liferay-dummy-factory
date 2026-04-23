package com.liferay.support.tools.workflow;

public record WorkflowStepExecutionResult(
	String stepId,
	String operation,
	WorkflowStepStatus status,
	WorkflowStepResult result,
	WorkflowError error) {

	public WorkflowStepExecutionResult {
		if ((stepId == null) || stepId.isBlank()) {
			throw new IllegalArgumentException("stepId is required");
		}

		if ((operation == null) || operation.isBlank()) {
			throw new IllegalArgumentException("operation is required");
		}

		if (status == null) {
			throw new IllegalArgumentException("status is required");
		}
	}

	public static WorkflowStepExecutionResult failed(
		String stepId, String operation, WorkflowError error,
		WorkflowStepResult result) {

		return new WorkflowStepExecutionResult(
			stepId, operation, WorkflowStepStatus.FAILED, result, error);
	}

	public static WorkflowStepExecutionResult notExecuted(
		String stepId, String operation) {

		return new WorkflowStepExecutionResult(
			stepId, operation, WorkflowStepStatus.NOT_EXECUTED, null, null);
	}

	public static WorkflowStepExecutionResult succeeded(
		String stepId, String operation, WorkflowStepResult result) {

		return new WorkflowStepExecutionResult(
			stepId, operation, WorkflowStepStatus.SUCCEEDED, result, null);
	}

}
