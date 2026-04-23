package com.liferay.support.tools.workflow;

public interface WorkflowFunction {

	public WorkflowStepExecutor executor();

	public String operation();

	public default void validateStep(WorkflowStepDefinition stepDefinition) {
	}

}
