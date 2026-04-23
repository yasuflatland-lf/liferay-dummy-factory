package com.liferay.support.tools.workflow;

import java.util.List;

public record WorkflowReference(
	String expression,
	WorkflowReferenceTarget target,
	String stepId,
	List<WorkflowReferenceSegment> segments) {

	public WorkflowReference {
		if ((expression == null) || expression.isBlank()) {
			throw new IllegalArgumentException("reference expression is required");
		}

		if (target == null) {
			throw new IllegalArgumentException("reference target is required");
		}

		if ((target == WorkflowReferenceTarget.STEP_RESULT) &&
			((stepId == null) || stepId.isBlank())) {

			throw new IllegalArgumentException("stepId is required");
		}

		segments = List.copyOf(segments);
	}

}
