package com.liferay.support.tools.workflow.adapter.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BlogsBatchSpec;
import com.liferay.support.tools.service.BlogsCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BlogsCreateWorkflowOperationAdapterTest {

	@Test
	void executeIgnoresUserIdParameter() throws Throwable {
		StubBlogsCreator blogsCreator = new StubBlogsCreator();
		BlogsCreateWorkflowOperationAdapter adapter =
			new BlogsCreateWorkflowOperationAdapter(blogsCreator);

		adapter.execute(
			new WorkflowExecutionContext(51L),
			Map.of("userId", 99L, "baseName", "Blog", "count", 1, "groupId", 801L));

		assertEquals(
			51L, blogsCreator._userId, "Creator must receive the context userId");
	}

	private static class StubBlogsCreator extends BlogsCreator {

		@Override
		public BatchResult<BlogsEntry> create(
			long userId, BlogsBatchSpec spec, ProgressCallback progress) {

			_userId = userId;

			return BatchResult.success(
				spec.batch().count(),
				List.of(TestModelProxyUtil.proxy(
					BlogsEntry.class, Map.of("getEntryId", 701L, "getTitle", "Blog"))),
				0);
		}

		private long _userId;

	}

}
