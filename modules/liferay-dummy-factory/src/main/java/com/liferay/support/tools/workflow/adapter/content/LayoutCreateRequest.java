package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

record LayoutCreateRequest(
	long userId,
	BatchSpec batchSpec,
	long groupId,
	String type,
	boolean privateLayout,
	boolean hidden) {

	static LayoutCreateRequest from(
		WorkflowExecutionContext workflowExecutionContext,
		WorkflowParameterValues workflowParameterValues) {

		WorkflowParameterValues.BatchInput batchInput =
			workflowParameterValues.requireBatchInput();

		return new LayoutCreateRequest(
			workflowExecutionContext.userId(),
			new BatchSpec(batchInput.count(), batchInput.baseName()),
			workflowParameterValues.requirePositiveLong("groupId"),
			workflowParameterValues.optionalString("type", "portlet"),
			workflowParameterValues.optionalBoolean("privateLayout", false),
			workflowParameterValues.optionalBoolean("hidden", false));
	}

}
