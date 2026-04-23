package com.liferay.support.tools.workflow;

public record WorkflowReferencePropertySegment(String property)
	implements WorkflowReferenceSegment {

	public WorkflowReferencePropertySegment {
		if ((property == null) || property.isBlank()) {
			throw new IllegalArgumentException("property is required");
		}
	}

}
