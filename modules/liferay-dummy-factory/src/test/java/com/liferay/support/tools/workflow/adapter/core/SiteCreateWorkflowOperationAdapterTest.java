package com.liferay.support.tools.workflow.adapter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.SiteMembershipType;
import com.liferay.support.tools.service.usecase.SiteCreateUseCase;
import com.liferay.support.tools.service.usecase.SiteItemResult;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SiteCreateWorkflowOperationAdapterTest {

	@Test
	void executeMapsInheritContentAndParentGroupId() throws Throwable {
		SiteCreateWorkflowOperationAdapter adapter =
			new SiteCreateWorkflowOperationAdapter(
				new _StubSiteCreateUseCase(
					List.of(
						new SiteItemResult(
							201L, "Child Site", true, 100L, null, null))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of(
				"count", 1,
				"baseName", "childsite",
				"parentGroupId", 100L,
				"inheritContent", true));

		assertTrue(result.success());
		assertEquals(1, result.count());
		assertEquals(
			List.of(
				Map.of(
					"groupId", 201L,
					"name", "Child Site",
					"inheritContent", true,
					"parentGroupId", 100L)),
			result.items());
		assertEquals("site.create", adapter.operationName());
	}

	@Test
	void executeMapsLayoutSetPrototypeUuids() throws Throwable {
		SiteCreateWorkflowOperationAdapter adapter =
			new SiteCreateWorkflowOperationAdapter(
				new _StubSiteCreateUseCase(
					List.of(
						new SiteItemResult(
							201L, "Proto Site", false, 0L,
							"pub-uuid-1", "priv-uuid-1"))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of(
				"count", 1,
				"baseName", "protosite",
				"publicLayoutSetPrototypeId", 301L,
				"privateLayoutSetPrototypeId", 302L));

		assertTrue(result.success());
		assertEquals(
			List.of(
				Map.of(
					"groupId", 201L,
					"name", "Proto Site",
					"inheritContent", false,
					"parentGroupId", 0L,
					"publicLayoutSetPrototypeUuid", "pub-uuid-1",
					"privateLayoutSetPrototypeUuid", "priv-uuid-1")),
			result.items());
	}

	@Test
	void executeSetsSkippedForPartialResults() throws Throwable {
		SiteCreateWorkflowOperationAdapter adapter =
			new SiteCreateWorkflowOperationAdapter(
				new _StubSiteCreateUseCase(
					List.of(
						new SiteItemResult(
							201L, "Site 1", false, 0L, null, null))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of("count", 2, "baseName", "testsite"));

		assertEquals(2, result.requested());
		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertFalse(result.success());
		assertEquals(
			"Only 1 of 2 sites were created.", result.error());
	}

	private static class _StubSiteCreateUseCase extends SiteCreateUseCase {

		@Override
		public BatchResult<SiteItemResult> create(
			long userId, long companyId, BatchSpec batchSpec,
			SiteMembershipType membershipType, long parentGroupId,
			long siteTemplateId, boolean manualMembership,
			boolean inheritContent, boolean active, String description,
			long publicLayoutSetPrototypeId, long privateLayoutSetPrototypeId,
			ProgressCallback progress) {

			int requested = batchSpec.count();

			if (_items.size() == requested) {
				return BatchResult.success(requested, _items, 0);
			}

			int skipped = requested - _items.size();

			return BatchResult.failure(
				requested, _items, skipped,
				"Only " + _items.size() + " of " + requested +
					" sites were created.");
		}

		private _StubSiteCreateUseCase(List<SiteItemResult> items) {
			_items = items;
		}

		private final List<SiteItemResult> _items;

	}

}
