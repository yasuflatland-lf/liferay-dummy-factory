package com.liferay.support.tools.workflow;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DefaultWorkflowExecutionContext
	implements WorkflowExecutionContextView {

	public DefaultWorkflowExecutionContext(Map<String, Object> input) {
		this(input, 0L, 0L);
	}

	public DefaultWorkflowExecutionContext(
		Map<String, Object> input, long currentUserId, long currentCompanyId) {

		_currentCompanyId = currentCompanyId;
		_currentUserId = currentUserId;
		_input = _immutableCopy(input);
	}

	public long currentCompanyId() {
		return _currentCompanyId;
	}

	public long currentUserId() {
		return _currentUserId;
	}

	@Override
	public Optional<WorkflowStepResult> getStepResult(String stepId) {
		return Optional.ofNullable(_stepResults.get(stepId));
	}

	@Override
	public Map<String, Object> input() {
		return _input;
	}

	public void putStepResult(String stepId, WorkflowStepResult result) {
		if ((stepId == null) || stepId.isBlank()) {
			throw new IllegalArgumentException("stepId is required");
		}

		if (result == null) {
			throw new IllegalArgumentException("result is required");
		}

		_stepResults.put(stepId, result);
	}

	private final long _currentCompanyId;
	private final long _currentUserId;
	private final Map<String, Object> _input;
	private final Map<String, WorkflowStepResult> _stepResults =
		new LinkedHashMap<>();

	private static Map<String, Object> _immutableCopy(
		Map<String, Object> source) {

		return Collections.unmodifiableMap(
			new LinkedHashMap<>((source == null) ? Map.of() : source));
	}

}
