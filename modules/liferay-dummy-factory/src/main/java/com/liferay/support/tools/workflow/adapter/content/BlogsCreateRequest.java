package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.BlogsBatchSpec;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

record BlogsCreateRequest(long userId, BlogsBatchSpec batchSpec) {

	static BlogsCreateRequest from(
		WorkflowExecutionContext workflowExecutionContext,
		WorkflowParameterValues workflowParameterValues) {

		WorkflowParameterValues.BatchInput batchInput =
			workflowParameterValues.requireBatchInput();
		long effectiveUserId = workflowParameterValues.optionalLong(
			"userId", workflowExecutionContext.userId());

		if (effectiveUserId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}

		return new BlogsCreateRequest(
			effectiveUserId,
			new BlogsBatchSpec(
				new BatchSpec(batchInput.count(), batchInput.baseName()),
				workflowParameterValues.requirePositiveLong("groupId"),
				workflowParameterValues.optionalString("content", ""),
				workflowParameterValues.optionalString("subtitle", ""),
				workflowParameterValues.optionalString("description", ""),
				workflowParameterValues.optionalBoolean("allowPingbacks", false),
				workflowParameterValues.optionalBoolean(
					"allowTrackbacks", false),
				workflowParameterValues.optionalStringArray(
					"trackbackURLs")));
	}

}
