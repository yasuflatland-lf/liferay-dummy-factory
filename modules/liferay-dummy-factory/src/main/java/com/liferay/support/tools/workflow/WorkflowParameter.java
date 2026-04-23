package com.liferay.support.tools.workflow;

public record WorkflowParameter(String name, WorkflowValueSource source) {

	public WorkflowParameter {
		if ((name == null) || name.isBlank()) {
			throw new IllegalArgumentException("parameter name is required");
		}

		if (source == null) {
			throw new IllegalArgumentException("parameter source is required");
		}
	}

}
