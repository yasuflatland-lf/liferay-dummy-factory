package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowStepExecutionRequestTest {

	@Test
	void constructorPreservesNullableParameters() {
		Map<String, Object> parameters = new LinkedHashMap<>();

		parameters.put("optionalValue", null);

		WorkflowStepExecutionRequest workflowStepExecutionRequest =
			new WorkflowStepExecutionRequest(
				"wf-1", "step-1", "sample.operation", "idem-1", parameters,
				new DefaultWorkflowExecutionContext(Map.of("userId", 1001L)));

		assertTrue(
			workflowStepExecutionRequest.parameters().containsKey(
				"optionalValue"));
		assertEquals(
			null, workflowStepExecutionRequest.parameters().get("optionalValue"));
	}

}
