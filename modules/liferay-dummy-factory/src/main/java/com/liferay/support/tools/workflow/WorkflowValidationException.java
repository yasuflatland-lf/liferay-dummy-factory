package com.liferay.support.tools.workflow;

import java.util.List;

public class WorkflowValidationException extends IllegalArgumentException {

	public WorkflowValidationException(List<WorkflowValidationError> errors) {
		super(_buildMessage(errors));

		_errors = (errors == null) ? List.of() : List.copyOf(errors);
	}

	public List<WorkflowValidationError> getErrors() {
		return _errors;
	}

	private static String _buildMessage(List<WorkflowValidationError> errors) {
		if ((errors == null) || errors.isEmpty()) {
			return "workflow validation failed";
		}

		WorkflowValidationError firstError = errors.get(0);

		return "workflow validation failed: " + firstError.path() + " " +
			firstError.message();
	}

	private final List<WorkflowValidationError> _errors;

}
