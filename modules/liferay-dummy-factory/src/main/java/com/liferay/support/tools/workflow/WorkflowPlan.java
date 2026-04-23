package com.liferay.support.tools.workflow;

import java.util.List;

public record WorkflowPlan(WorkflowDefinition definition) {

	public WorkflowPlan {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
	}

	public List<WorkflowStepDefinition> orderedSteps() {
		return definition.steps();
	}

}
