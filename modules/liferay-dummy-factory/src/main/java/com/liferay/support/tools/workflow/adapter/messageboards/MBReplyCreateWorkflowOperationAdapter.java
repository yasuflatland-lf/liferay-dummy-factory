package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBReplyBatchSpec;
import com.liferay.support.tools.service.MBReplyCreator;
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

@Component(service = WorkflowOperationAdapter.class)
public class MBReplyCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	public MBReplyCreateWorkflowOperationAdapter() {
	}

	MBReplyCreateWorkflowOperationAdapter(MBReplyCreator mbReplyCreator) {
		_mbReplyCreator = mbReplyCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		MBReplyCreateRequest request = new MBReplyCreateRequest(
			_effectiveUserId(values, workflowExecutionContext),
			values.requirePositiveLong("threadId"),
			values.requireCount(), values.requireText("body"),
			values.optionalString("format", "html"),
			values.optionalBoolean("fakerEnable", false),
			values.optionalString("locale", "en_US"));

		MBReplyBatchSpec spec = new MBReplyBatchSpec(
			new BatchSpec(request.count(), "reply"), request.threadId(),
			request.body(), request.format(), request.fakerEnable(),
			request.locale());

		BatchResult<MBMessage> result = _mbReplyCreator.create(
			request.userId(), spec, ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			reply -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("body", reply.getBody());
				item.put("messageId", reply.getMessageId());
				item.put("subject", reply.getSubject());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "mbReply.create";
	}

	private static long _effectiveUserId(
		WorkflowParameterValues values,
		WorkflowExecutionContext workflowExecutionContext) {

		long userId = values.optionalLong(
			"userId", workflowExecutionContext.userId());

		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}

		return userId;
	}

	@Reference
	private MBReplyCreator _mbReplyCreator;

}
