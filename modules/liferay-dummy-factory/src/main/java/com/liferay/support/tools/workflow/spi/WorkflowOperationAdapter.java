package com.liferay.support.tools.workflow.spi;

import java.util.Map;

public interface WorkflowOperationAdapter {

	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable;

	public String operationName();

}
