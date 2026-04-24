package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.AssetTagNames;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBThreadBatchSpec;
import com.liferay.support.tools.service.MBThreadCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.WorkflowResultNormalizer;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=mbThread.create",
	service = WorkflowOperationAdapter.class
)
public class MBThreadCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	public static final String OPERATION = "mbThread.create";

	public MBThreadCreateWorkflowOperationAdapter() {
	}

	MBThreadCreateWorkflowOperationAdapter(MBThreadCreator mbThreadCreator) {
		_mbThreadCreator = mbThreadCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		MBThreadCreateRequest request = new MBThreadCreateRequest(
			_effectiveUserId(values, workflowExecutionContext),
			values.requirePositiveLong("groupId"),
			values.optionalLong("categoryId", 0L), _batchSpec(values),
			values.requireText("body"), values.optionalString("format", "html"));

		MBThreadBatchSpec spec = new MBThreadBatchSpec(
			request.batch(), request.groupId(), request.categoryId(),
			request.body(), request.format(), AssetTagNames.EMPTY);

		BatchResult<MBMessage> result = _mbThreadCreator.create(
			request.userId(), spec, ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			message -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("categoryId", message.getCategoryId());
				item.put("groupId", message.getGroupId());
				item.put("messageId", message.getMessageId());
				item.put("subject", message.getSubject());
				item.put("threadId", message.getThreadId());

				return item;
			});
	}

	@Override
	public String operationName() {
		return OPERATION;
	}

	private static BatchSpec _batchSpec(WorkflowParameterValues values) {
		return new BatchSpec(values.requireCount(), values.requireText("baseName"));
	}

	private static long _effectiveUserId(
		WorkflowParameterValues values,
		WorkflowExecutionContext workflowExecutionContext) {

		long userId = values.optionalLong("userId", workflowExecutionContext.userId());

		if (userId <= 0) {
			throw new IllegalArgumentException("userId is required");
		}

		return userId;
	}

	@Reference
	private MBThreadCreator _mbThreadCreator;

}
