package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

record DocumentCreateRequest(
	long userId,
	BatchSpec batchSpec,
	long groupId,
	long folderId,
	String description,
	String[] uploadedFiles) {

	static DocumentCreateRequest from(
		WorkflowExecutionContext workflowExecutionContext,
		WorkflowParameterValues workflowParameterValues) {

		WorkflowParameterValues.BatchInput batchInput =
			workflowParameterValues.requireBatchInput();

		return new DocumentCreateRequest(
			workflowExecutionContext.userId(),
			new BatchSpec(batchInput.count(), batchInput.baseName()),
			workflowParameterValues.requirePositiveLong("groupId"),
			workflowParameterValues.optionalLong("folderId", 0L),
			workflowParameterValues.optionalString("description", ""),
			workflowParameterValues.optionalStringArray("uploadedFiles"));
	}

}
