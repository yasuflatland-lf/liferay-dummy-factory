package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;

import java.util.Map;

abstract class AbstractCreatorWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	protected WorkflowParameterValues parameters(Map<String, Object> parameters) {
		return new WorkflowParameterValues(parameters);
	}

	protected ProgressCallback progressCallback() {
		return ProgressCallback.NOOP;
	}

}
