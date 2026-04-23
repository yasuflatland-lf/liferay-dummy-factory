package com.liferay.support.tools.workflow;

public interface WorkflowStepExecutor {

	public WorkflowStepResult execute(
			WorkflowStepExecutionRequest workflowStepExecutionRequest)
		throws Exception;

}
