package com.liferay.support.tools.workflow.adapter.taxonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.CategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.CategoryCreateRequest;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CategoryCreateWorkflowOperationAdapterTest {

	@Test
	void executeMapsCreatedCategoriesIntoStepResult() throws Throwable {
		StubCategoryCreator categoryCreator = new StubCategoryCreator(
			List.of(
				_assetCategory(101L, 201L, 301L, "Category 1"),
				_assetCategory(102L, 201L, 301L, "Category 2")));

		CategoryCreateWorkflowOperationAdapter adapter =
			new CategoryCreateWorkflowOperationAdapter(categoryCreator);
		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(11L),
			Map.of(
				"count", 2,
				"baseName", "Category",
				"groupId", 201L,
				"vocabularyId", 301L));

		assertTrue(result.success());
		assertEquals(2, result.requested());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				Map.of(
					"categoryId", 101L,
					"groupId", 201L,
					"vocabularyId", 301L,
					"name", "Category 1"),
				Map.of(
					"categoryId", 102L,
					"groupId", 201L,
					"vocabularyId", 301L,
					"name", "Category 2")),
			result.items());
		assertEquals(11L, categoryCreator.userId);
		assertEquals(201L, categoryCreator.groupId);
		assertEquals(301L, categoryCreator.vocabularyId);
		assertEquals(2, categoryCreator.batchSpec.count());
		assertEquals("Category", categoryCreator.batchSpec.baseName());
		assertSame(ProgressCallback.NOOP, categoryCreator.progressCallback);
	}

	@Test
	void executeHonorsUserIdOverride() throws Throwable {
		StubCategoryCreator categoryCreator = new StubCategoryCreator(
			List.of(_assetCategory(101L, 201L, 301L, "Category 1")));

		CategoryCreateWorkflowOperationAdapter adapter =
			new CategoryCreateWorkflowOperationAdapter(categoryCreator);
		adapter.execute(
			new WorkflowExecutionContext(11L),
			Map.of(
				"userId", 99L,
				"count", 1,
				"baseName", "Category",
				"groupId", 201L,
				"vocabularyId", 301L));

		assertEquals(99L, categoryCreator.userId);
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		CategoryCreateWorkflowOperationAdapter adapter =
			new CategoryCreateWorkflowOperationAdapter(
				new StubCategoryCreator(
					List.of(_assetCategory(101L, 201L, 301L, "Category 1"))));

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(11L),
			Map.of(
				"count", 2,
				"baseName", "Category",
				"groupId", 201L,
				"vocabularyId", 301L));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 categories were created.", result.error());
	}

	@Test
	void requestRejectsInvalidVocabularyId() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new CategoryCreateRequest(
				11L, 201L, 0L, new BatchSpec(1, "Category")));
	}

	private static AssetCategory _assetCategory(
		long categoryId, long groupId, long vocabularyId, String name) {

		return TestModelProxyUtil.proxy(
			AssetCategory.class,
			Map.of(
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getName", name,
				"getVocabularyId", vocabularyId));
	}

	private static class StubCategoryCreator extends CategoryCreator {

		@Override
		public BatchResult<AssetCategory> create(
			long userId, long groupId, long vocabularyId, BatchSpec batchSpec,
			ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.groupId = groupId;
			this.progressCallback = progress;
			this.userId = userId;
			this.vocabularyId = vocabularyId;

			int requested = batchSpec.count();

			if (_categories.size() == requested) {
				return BatchResult.success(requested, _categories, 0);
			}

			int skipped = requested - _categories.size();

			return BatchResult.failure(
				requested, _categories, skipped,
				"Only " + _categories.size() + " of " + requested +
					" categories were created.");
		}

		private StubCategoryCreator(List<AssetCategory> categories) {
			_categories = categories;
		}

		private final List<AssetCategory> _categories;
		private BatchSpec batchSpec;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;
		private long vocabularyId;

	}

}
