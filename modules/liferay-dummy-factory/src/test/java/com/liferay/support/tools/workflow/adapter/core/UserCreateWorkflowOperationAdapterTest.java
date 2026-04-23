package com.liferay.support.tools.workflow.adapter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.UserBatchSpec;
import com.liferay.support.tools.service.usecase.UserCreateUseCase;
import com.liferay.support.tools.service.usecase.UserItemResult;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UserCreateWorkflowOperationAdapterTest {

	@Test
	void executeMapsGroupIdAndLayoutUuidsWhenPresent() throws Throwable {
		UserCreateWorkflowOperationAdapter adapter =
			new UserCreateWorkflowOperationAdapter(
				new _StubUserCreateUseCase(
					List.of(
						new UserItemResult(
							"user1@test.com", "user1", 101L,
							201L, "pub-uuid-1", "priv-uuid-1"))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of(
				"count", 1,
				"baseName", "testuser",
				"generatePersonalSiteLayouts", true,
				"publicLayoutSetPrototypeId", 301L));

		assertTrue(result.success());
		assertEquals(1, result.count());
		assertEquals(
			List.of(
				Map.of(
					"emailAddress", "user1@test.com",
					"screenName", "user1",
					"userId", 101L,
					"groupId", 201L,
					"publicLayoutSetPrototypeUuid", "pub-uuid-1",
					"privateLayoutSetPrototypeUuid", "priv-uuid-1")),
			result.items());
		assertEquals("user.create", adapter.operationName());
	}

	@Test
	void executeOmitsGroupIdAndUuidsWhenNotGenerated() throws Throwable {
		UserCreateWorkflowOperationAdapter adapter =
			new UserCreateWorkflowOperationAdapter(
				new _StubUserCreateUseCase(
					List.of(
						new UserItemResult(
							"user1@test.com", "user1", 101L,
							0L, null, null))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of("count", 1, "baseName", "testuser"));

		assertTrue(result.success());
		assertEquals(
			List.of(
				Map.of(
					"emailAddress", "user1@test.com",
					"screenName", "user1",
					"userId", 101L)),
			result.items());
	}

	@Test
	void executeSetsSkippedForPartialResults() throws Throwable {
		UserCreateWorkflowOperationAdapter adapter =
			new UserCreateWorkflowOperationAdapter(
				new _StubUserCreateUseCase(
					List.of(
						new UserItemResult(
							"user1@test.com", "user1", 101L,
							0L, null, null))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of("count", 2, "baseName", "testuser"));

		assertEquals(2, result.requested());
		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertFalse(result.success());
		assertEquals(
			"Only 1 of 2 users were created.", result.error());
	}

	private static class _StubUserCreateUseCase extends UserCreateUseCase {

		@Override
		public BatchResult<UserItemResult> create(
			long creatorUserId, long companyId, UserBatchSpec spec,
			ProgressCallback progress) {

			int requested = spec.batch().count();

			if (_items.size() == requested) {
				return BatchResult.success(requested, _items, 0);
			}

			int skipped = requested - _items.size();

			return BatchResult.failure(
				requested, _items, skipped,
				"Only " + _items.size() + " of " + requested +
					" users were created.");
		}

		private _StubUserCreateUseCase(List<UserItemResult> items) {
			_items = items;
		}

		private final List<UserItemResult> _items;

	}

}
