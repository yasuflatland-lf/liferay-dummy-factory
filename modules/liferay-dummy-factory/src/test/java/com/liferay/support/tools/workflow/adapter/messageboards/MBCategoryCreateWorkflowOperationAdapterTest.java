package com.liferay.support.tools.workflow.adapter.messageboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBCategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MBCategoryCreateWorkflowOperationAdapterTest {

	@Test
	void executeMapsCreatedCategoriesIntoStepResult() throws Throwable {
		StubMBCategoryCreator mbCategoryCreator = new StubMBCategoryCreator(
			List.of(
				_mbCategory(701L, 801L, "MB Category 1"),
				_mbCategory(702L, 801L, "MB Category 2")));

		MBCategoryCreateWorkflowOperationAdapter adapter =
			new MBCategoryCreateWorkflowOperationAdapter(mbCategoryCreator);

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(51L),
			Map.of(
				"baseName", "MB Category", "count", 2, "description",
				"desc", "groupId", 801L));

		assertEquals(
			MBCategoryCreateWorkflowOperationAdapter.OPERATION,
			adapter.operationName());
		assertTrue(result.success());
		assertEquals(2, result.requested());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				Map.of(
					"categoryId", 701L, "groupId", 801L, "name",
					"MB Category 1"),
				Map.of(
					"categoryId", 702L, "groupId", 801L, "name",
					"MB Category 2")),
			result.items());
		assertEquals(51L, mbCategoryCreator.userId);
		assertEquals(801L, mbCategoryCreator.groupId);
		assertEquals("desc", mbCategoryCreator.description);
		assertSame(ProgressCallback.NOOP, mbCategoryCreator.progressCallback);
	}

	@Test
	void executeHonorsUserIdOverride() throws Throwable {
		StubMBCategoryCreator mbCategoryCreator = new StubMBCategoryCreator(
			List.of(_mbCategory(701L, 801L, "MB Category 1")));

		MBCategoryCreateWorkflowOperationAdapter adapter =
			new MBCategoryCreateWorkflowOperationAdapter(mbCategoryCreator);
		adapter.execute(
			new WorkflowExecutionContext(51L),
			Map.of(
				"userId", 99L,
				"baseName", "MB Category", "count", 1, "description",
				"desc", "groupId", 801L));

		assertEquals(99L, mbCategoryCreator.userId);
	}

	@Test
	void requestRejectsMissingDescription() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBCategoryCreateRequest(
				51L, 801L, new BatchSpec(1, "MB Category"), null));
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		MBCategoryCreateWorkflowOperationAdapter adapter =
			new MBCategoryCreateWorkflowOperationAdapter(
				new StubMBCategoryCreator(
					List.of(_mbCategory(701L, 801L, "MB Category 1"))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(51L),
			Map.of(
				"baseName", "MB Category", "count", 2, "description",
				"desc", "groupId", 801L));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 MB categories were created.", result.error());
	}

	private static MBCategory _mbCategory(
		long categoryId, long groupId, String name) {

		return TestModelProxyUtil.proxy(
			MBCategory.class,
			Map.of(
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getName", name));
	}

	private static class StubMBCategoryCreator extends MBCategoryCreator {

		@Override
		public BatchResult<MBCategory> create(
			long userId, long groupId, BatchSpec batchSpec, String description,
			ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.description = description;
			this.groupId = groupId;
			this.progressCallback = progress;
			this.userId = userId;

			int requested = batchSpec.count();

			if (_categories.size() == requested) {
				return BatchResult.success(requested, _categories, 0);
			}

			int skipped = requested - _categories.size();

			return BatchResult.failure(
				requested, _categories, skipped,
				"Only " + _categories.size() + " of " + requested +
					" MB categories were created.");
		}

		private StubMBCategoryCreator(List<MBCategory> categories) {
			_categories = categories;
		}

		private final List<MBCategory> _categories;
		private BatchSpec batchSpec;
		private String description;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;

	}

}
