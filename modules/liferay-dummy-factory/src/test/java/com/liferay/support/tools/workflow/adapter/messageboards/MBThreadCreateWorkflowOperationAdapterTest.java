package com.liferay.support.tools.workflow.adapter.messageboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBThreadBatchSpec;
import com.liferay.support.tools.service.MBThreadCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MBThreadCreateWorkflowOperationAdapterTest {

	@Test
	void executeMapsCreatedThreadsIntoStepResult() throws Throwable {
		StubMBThreadCreator mbThreadCreator = new StubMBThreadCreator(
			List.of(
				_mbMessage(901L, 1001L, 1101L, 1201L, "Thread 1"),
				_mbMessage(902L, 1001L, 1101L, 1202L, "Thread 2")));

		MBThreadCreateWorkflowOperationAdapter adapter =
			new MBThreadCreateWorkflowOperationAdapter(mbThreadCreator);

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(61L),
			Map.of(
				"baseName", "Thread", "body", "body", "categoryId", 1101L,
				"count", 2, "groupId", 1001L));

		assertEquals(
			MBThreadCreateWorkflowOperationAdapter.OPERATION,
			adapter.operationName());
		assertTrue(result.success());
		assertEquals(2, result.requested());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				Map.of(
					"categoryId", 1101L, "groupId", 1001L, "messageId",
					901L, "subject", "Thread 1", "threadId", 1201L),
				Map.of(
					"categoryId", 1101L, "groupId", 1001L, "messageId",
					902L, "subject", "Thread 2", "threadId", 1202L)),
			result.items());
		assertEquals(61L, mbThreadCreator.userId);
		assertEquals(1001L, mbThreadCreator.groupId);
		assertEquals(1101L, mbThreadCreator.categoryId);
		assertEquals("body", mbThreadCreator.body);
		assertEquals("html", mbThreadCreator.format);
		assertSame(ProgressCallback.NOOP, mbThreadCreator.progressCallback);
	}

	@Test
	void executeHonorsUserIdOverride() throws Throwable {
		StubMBThreadCreator mbThreadCreator = new StubMBThreadCreator(
			List.of(_mbMessage(901L, 1001L, 1101L, 1201L, "Thread 1")));

		MBThreadCreateWorkflowOperationAdapter adapter =
			new MBThreadCreateWorkflowOperationAdapter(mbThreadCreator);
		adapter.execute(
			new WorkflowExecutionContext(61L),
			Map.of(
				"userId", 99L,
				"baseName", "Thread", "body", "body", "categoryId", 1101L,
				"count", 1, "groupId", 1001L));

		assertEquals(99L, mbThreadCreator.userId);
	}

	@Test
	void requestRejectsNegativeCategoryId() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBThreadCreateRequest(
				61L, 1001L, -1L, new BatchSpec(1, "Thread"), "body", ""));
	}

	@Test
	void requestRejectsMissingBody() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBThreadCreateRequest(
				61L, 1001L, 1101L, new BatchSpec(1, "Thread"), null, ""));
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		MBThreadCreateWorkflowOperationAdapter adapter =
			new MBThreadCreateWorkflowOperationAdapter(
				new StubMBThreadCreator(
					List.of(_mbMessage(901L, 1001L, 1101L, 1201L, "Thread 1"))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(61L),
			Map.of(
				"baseName", "Thread", "body", "body", "categoryId", 1101L,
				"count", 2, "format", "markdown", "groupId", 1001L));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 MB threads were created.", result.error());
	}

	private static MBMessage _mbMessage(
		long messageId, long groupId, long categoryId, long threadId,
		String subject) {

		return TestModelProxyUtil.proxy(
			MBMessage.class,
			Map.of(
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getMessageId", messageId,
				"getSubject", subject,
				"getThreadId", threadId));
	}

	private static class StubMBThreadCreator extends MBThreadCreator {

		@Override
		public BatchResult<MBMessage> create(
				long userId, MBThreadBatchSpec spec, ProgressCallback progress)
			throws Throwable {

			this.batchSpec = spec.batch();
			this.body = spec.body();
			this.categoryId = spec.categoryId();
			this.format = spec.format();
			this.groupId = spec.groupId();
			this.progressCallback = progress;
			this.userId = userId;

			int requested = spec.batch().count();

			if (_messages.size() == requested) {
				return BatchResult.success(requested, _messages, 0);
			}

			int skipped = requested - _messages.size();

			return BatchResult.failure(
				requested, _messages, skipped,
				"Only " + _messages.size() + " of " + requested +
					" MB threads were created.");
		}

		private StubMBThreadCreator(List<MBMessage> messages) {
			_messages = messages;
		}

		private final List<MBMessage> _messages;
		private BatchSpec batchSpec;
		private String body;
		private long categoryId;
		private String format;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;

	}

}
