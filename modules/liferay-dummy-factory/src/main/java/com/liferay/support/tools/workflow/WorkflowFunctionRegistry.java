package com.liferay.support.tools.workflow;

import java.util.Optional;

public interface WorkflowFunctionRegistry {

	public Optional<WorkflowFunction> getFunction(String operation);

	public default WorkflowFunction getRequiredFunction(String operation) {
		return getFunction(
		operation
		).orElseThrow(
			() -> new IllegalArgumentException(
				"unknown workflow operation: " + operation));
	}

}
