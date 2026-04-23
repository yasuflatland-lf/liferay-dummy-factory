package com.liferay.support.tools.workflow;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

public record WorkflowStepExecutionRequest(
	String workflowId,
	String stepId,
	String operation,
	String idempotencyKey,
	Map<String, Object> parameters,
	WorkflowExecutionContextView context) {

	public WorkflowStepExecutionRequest {
		if ((stepId == null) || stepId.isBlank()) {
			throw new IllegalArgumentException("stepId is required");
		}

		if ((operation == null) || operation.isBlank()) {
			throw new IllegalArgumentException("operation is required");
		}

		if ((idempotencyKey == null) || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("idempotencyKey is required");
		}

		parameters = Collections.unmodifiableMap(
			new LinkedHashMap<>(parameters));

		if (context == null) {
			throw new IllegalArgumentException("context is required");
		}
	}

}
