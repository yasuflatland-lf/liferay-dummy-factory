package com.liferay.support.tools.workflow;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultWorkflowFunction implements WorkflowFunction {

	public DefaultWorkflowFunction(
		WorkflowFunctionDescriptor descriptor, WorkflowStepExecutor executor) {

		_descriptor = Objects.requireNonNull(
			descriptor, "descriptor is required");
		_executor = Objects.requireNonNull(executor, "executor is required");
	}

	public WorkflowFunctionDescriptor descriptor() {
		return _descriptor;
	}

	@Override
	public WorkflowStepExecutor executor() {
		return _executor;
	}

	@Override
	public String operation() {
		return _descriptor.operation();
	}

	@Override
	public void validateStep(WorkflowStepDefinition stepDefinition) {
		Set<String> parameterNames = stepDefinition.parameters().stream(
		).map(
			WorkflowParameter::name
		).collect(
			Collectors.toCollection(LinkedHashSet::new)
		);

		Set<String> missingRequiredParameters = _descriptor.parameters().stream(
		).filter(
			WorkflowFunctionParameter::required
		).map(
			WorkflowFunctionParameter::name
		).filter(
			name -> !parameterNames.contains(name)
		).collect(
			Collectors.toCollection(LinkedHashSet::new)
		);

		if (!missingRequiredParameters.isEmpty()) {
			throw new IllegalArgumentException(
				"missing required parameters: " +
					String.join(", ", missingRequiredParameters));
		}
	}

	private final WorkflowFunctionDescriptor _descriptor;
	private final WorkflowStepExecutor _executor;

}
