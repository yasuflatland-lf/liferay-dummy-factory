package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
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

		long userId = workflowExecutionContext.userId();

		// Honor explicit per-step override if present; schema documents `userId`
		// as an optional parameter for impersonation-like workflows.
		if ((parameters != null) && parameters.containsKey("userId")) {
			long overrideUserId = values.optionalLong("userId", Long.MIN_VALUE);

			if (overrideUserId != Long.MIN_VALUE) {
				if (overrideUserId <= 0) {
					throw new IllegalArgumentException(
						"userId must be positive");
				}

				userId = overrideUserId;
			}
		}

		MBReplyCreateRequest request = new MBReplyCreateRequest(
			userId, values.requirePositiveLong("threadId"),
			values.requireCount(), values.requireText("body"),
			values.optionalString("format", "html"));

		BatchResult<MBMessage> result = _mbReplyCreator.create(
			request.userId(), request.threadId(),
			new BatchSpec(request.count(), "reply"),
			request.body(), request.format(), ProgressCallback.NOOP);

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

	@Reference
	private MBReplyCreator _mbReplyCreator;

}
