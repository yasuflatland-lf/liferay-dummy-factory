package com.liferay.support.tools.workflow.adapter.messageboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBReplyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MBReplyCreateWorkflowOperationAdapterTest {

	@Test
	void executeMapsCreatedRepliesIntoStepResult() throws Throwable {
		StubMBReplyCreator mbReplyCreator = new StubMBReplyCreator(
			List.of(
				_mbReply(1301L, 1401L, 1501L, 1601L, "Reply 1", "Body 1"),
				_mbReply(1302L, 1401L, 1501L, 1601L, "Reply 2", "Body 2")));

		MBReplyCreateWorkflowOperationAdapter adapter =
			new MBReplyCreateWorkflowOperationAdapter(mbReplyCreator);

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(71L),
			Map.of("body", "reply body", "count", 2, "threadId", 1601L));

		assertEquals("mbReply.create", adapter.operationName());
		assertTrue(result.success());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				Map.of(
					"body", "Body 1", "messageId", 1301L, "subject",
					"Reply 1"),
				Map.of(
					"body", "Body 2", "messageId", 1302L, "subject",
					"Reply 2")),
			result.items());
		assertEquals(71L, mbReplyCreator.userId);
		assertEquals(1601L, mbReplyCreator.threadId);
		assertEquals("reply body", mbReplyCreator.body);
		assertEquals("html", mbReplyCreator.format);
		assertSame(ProgressCallback.NOOP, mbReplyCreator.progressCallback);
	}

	@Test
	void executeHonorsUserIdOverrideParameter() throws Throwable {
		StubMBReplyCreator mbReplyCreator = new StubMBReplyCreator(
			List.of(_mbReply(1301L, 1401L, 1501L, 1601L, "Reply 1", "Body 1")));

		MBReplyCreateWorkflowOperationAdapter adapter =
			new MBReplyCreateWorkflowOperationAdapter(mbReplyCreator);

		adapter.execute(
			new WorkflowExecutionContext(71L),
			Map.of(
				"body", "reply body", "count", 1, "threadId", 1601L, "userId",
				72L));

		assertEquals(72L, mbReplyCreator.userId);
	}

	@Test
	void requestRejectsInvalidCount() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBReplyCreateRequest(71L, 1601L, 0, "reply body", ""));
	}

	@Test
	void requestRejectsMissingBody() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBReplyCreateRequest(71L, 1601L, 1, null, ""));
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		MBReplyCreateWorkflowOperationAdapter adapter =
			new MBReplyCreateWorkflowOperationAdapter(
				new StubMBReplyCreator(
					List.of(
						_mbReply(
							1301L, 1401L, 1501L, 1601L, "Reply 1", "Body 1"))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(71L),
			Map.of(
				"body", "reply body", "count", 2, "format", "markdown",
				"threadId", 1601L));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 MB replies were created.", result.error());
	}

	private static MBMessage _mbReply(
		long messageId, long groupId, long categoryId, long threadId,
		String subject, String body) {

		return TestModelProxyUtil.proxy(
			MBMessage.class,
			Map.of(
				"getBody", body,
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getMessageId", messageId,
				"getSubject", subject,
				"getThreadId", threadId));
	}

	private static class StubMBReplyCreator extends MBReplyCreator {

		@Override
		public BatchResult<MBMessage> create(
			long userId, long threadId, BatchSpec batchSpec, String body,
			String format, ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.body = body;
			this.format = format;
			this.progressCallback = progress;
			this.threadId = threadId;
			this.userId = userId;

			int requested = batchSpec.count();

			if (_replies.size() == requested) {
				return BatchResult.success(requested, _replies, 0);
			}

			int skipped = requested - _replies.size();

			return BatchResult.failure(
				requested, _replies, skipped,
				"Only " + _replies.size() + " of " + requested +
					" MB replies were created.");
		}

		private StubMBReplyCreator(List<MBMessage> replies) {
			_replies = replies;
		}

		private BatchSpec batchSpec;
		private String body;
		private String format;
		private ProgressCallback progressCallback;
		private final List<MBMessage> _replies;
		private long threadId;
		private long userId;

	}

}
