package com.liferay.support.tools.workflow;

public record WorkflowReferenceIndexSegment(int index)
	implements WorkflowReferenceSegment {

	public WorkflowReferenceIndexSegment {
		if (index < 0) {
			throw new IllegalArgumentException("index must be greater than or equal to 0");
		}
	}

}
