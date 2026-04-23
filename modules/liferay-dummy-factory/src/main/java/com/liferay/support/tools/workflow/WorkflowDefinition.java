package com.liferay.support.tools.workflow;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record WorkflowDefinition(
	String schemaVersion,
	String workflowId,
	Map<String, Object> input,
	List<WorkflowStepDefinition> steps) {

	public WorkflowDefinition {
		if ((schemaVersion == null) || schemaVersion.isBlank()) {
			throw new IllegalArgumentException("schemaVersion is required");
		}

		if (input == null) {
			throw new IllegalArgumentException("input is required");
		}

		if (steps == null) {
			throw new IllegalArgumentException("steps is required");
		}

		input = Collections.unmodifiableMap(new LinkedHashMap<>(input));
		steps = List.copyOf(steps);
	}

}
