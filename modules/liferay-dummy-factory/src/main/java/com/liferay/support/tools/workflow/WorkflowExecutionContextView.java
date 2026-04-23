package com.liferay.support.tools.workflow;

import java.util.Map;
import java.util.Optional;

public interface WorkflowExecutionContextView {

	public Optional<WorkflowStepResult> getStepResult(String stepId);

	public Map<String, Object> input();

}
