package com.liferay.support.tools.workflow;

public record WorkflowFunctionParameter(
	String name,
	String type,
	boolean required,
	String description,
	Object defaultValue) {

	public WorkflowFunctionParameter {
		if ((name == null) || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}

		if ((type == null) || type.isBlank()) {
			throw new IllegalArgumentException("type is required");
		}

		if ((description == null) || description.isBlank()) {
			throw new IllegalArgumentException("description is required");
		}
	}

}
