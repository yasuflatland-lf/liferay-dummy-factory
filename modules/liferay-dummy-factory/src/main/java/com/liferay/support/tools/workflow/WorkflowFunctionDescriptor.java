package com.liferay.support.tools.workflow;

import java.util.List;

public record WorkflowFunctionDescriptor(
	String operation,
	String description,
	List<WorkflowFunctionParameter> parameters,
	String resultShape) {

	public WorkflowFunctionDescriptor {
		if ((operation == null) || operation.isBlank()) {
			throw new IllegalArgumentException("operation is required");
		}

		if ((description == null) || description.isBlank()) {
			throw new IllegalArgumentException("description is required");
		}

		if (parameters == null) {
			throw new IllegalArgumentException("parameters is required");
		}

		if ((resultShape == null) || resultShape.isBlank()) {
			throw new IllegalArgumentException("resultShape is required");
		}

		parameters = List.copyOf(parameters);
	}

}
