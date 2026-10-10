package com.liferay.support.tools.workflow.adapter.content;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.WebContentBatchSpec;
import com.liferay.support.tools.service.WebContentCreator;
import com.liferay.support.tools.service.WebContentPerSiteResult;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WebContentCreateWorkflowOperationAdapterTest {

	@Test
	void executePreservesPerSiteItemsWithTotalArticleCount() throws Throwable {
		_StubWebContentCreator creator = new _StubWebContentCreator();
		WebContentCreateWorkflowOperationAdapter adapter =
			new WebContentCreateWorkflowOperationAdapter(null, creator);

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(41L),
			Map.of(
				"count", 3, "baseName", "Article", "groupIds", List.of(201L),
				"locales", List.of("en_US"), "createContentsType", 0,
				"baseArticle", "Workflow body", "folderId", 0));

		assertTrue(result.success());
		assertEquals(3, result.requested());
		assertEquals(3, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(1, result.items().size());
		assertEquals(
			Map.of("groupId", 201L, "siteName", "Site", "created", 3, "failed", 0),
			result.items().get(0));
		assertFalse(result.items().get(0).containsKey("error"));
		assertEquals("webContent.create", adapter.operationName());
		assertEquals(41L, creator._userId);
		assertEquals(3, creator._spec.batch().count());
		assertEquals("Article", creator._spec.batch().baseName());
		assertArrayEquals(new long[] {201L}, creator._spec.groupIds());
		assertArrayEquals(new String[] {"en_US"}, creator._spec.locales());
		assertSame(ProgressCallback.NOOP, creator._progress);
	}

	@Test
	void executeKeepsPerSiteErrorWhenCountDiffersFromItemsOnFailure()
		throws Throwable {

		_StubWebContentCreator creator = new _StubWebContentCreator(
			new BatchResult<>(
				false, 0, 2, 2,
				List.of(
					new WebContentPerSiteResult(
						201L, "Site", 0, 2, "No DDMStructure exists")),
				"No DDMStructure exists"));
		WebContentCreateWorkflowOperationAdapter adapter =
			new WebContentCreateWorkflowOperationAdapter(null, creator);

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(41L),
			Map.of(
				"count", 2, "baseName", "Article", "groupIds", List.of(201L),
				"locales", List.of("en_US"), "createContentsType", 2,
				"ddmStructureId", 999L, "ddmTemplateId", 999L, "folderId", 0));

		assertFalse(result.success());
		assertEquals(2, result.requested());
		assertEquals(0, result.count());
		assertEquals(2, result.skipped());
		assertEquals("No DDMStructure exists", result.error());
		assertEquals(1, result.items().size());
		assertEquals(
			Map.of(
				"groupId", 201L, "siteName", "Site", "created", 0, "failed", 2,
				"error", "No DDMStructure exists"),
			result.items().get(0));
	}

	private static class _StubWebContentCreator extends WebContentCreator {

		_StubWebContentCreator() {
			this(
				new BatchResult<>(
					true, 3, 3, 0,
					List.of(new WebContentPerSiteResult(201L, "Site", 3, 0, null)),
					null));
		}

		_StubWebContentCreator(BatchResult<WebContentPerSiteResult> result) {
			_result = result;
		}

		@Override
		public BatchResult<WebContentPerSiteResult> create(
			long userId, WebContentBatchSpec spec, ProgressCallback progress) {

			_userId = userId;
			_spec = spec;
			_progress = progress;

			return _result;
		}

		private ProgressCallback _progress;
		private final BatchResult<WebContentPerSiteResult> _result;
		private WebContentBatchSpec _spec;
		private long _userId;

	}

}
