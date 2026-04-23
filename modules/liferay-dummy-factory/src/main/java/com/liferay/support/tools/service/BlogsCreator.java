package com.liferay.support.tools.service;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.blogs.service.BlogsEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = BlogsCreator.class)
public class BlogsCreator {

	public BatchResult<BlogsEntry> create(
			long userId, BlogsBatchSpec spec, ProgressCallback progress)
		throws Throwable {

		BatchSpec batch = spec.batch();
		int count = batch.count();

		List<BlogsEntry> created = new ArrayList<>(count);
		int skipped = 0;

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(spec.groupId());
		serviceContext.setUserId(userId);

		Date displayDate = new Date();

		for (int i = 0; i < count; i++) {
			final String title = BatchNaming.resolve(
				batch.baseName(), count, i, " ");

			try {
				BlogsEntry entry = BatchTransaction.run(
					() -> _blogsEntryLocalService.addEntry(
						userId, title, spec.subtitle(), spec.description(),
						spec.content(), displayDate, spec.allowPingbacks(),
						spec.allowTrackbacks(), spec.trackbackURLs(),
						StringPool.BLANK, null, null, serviceContext));

				created.add(entry);
			}
			catch (Exception e) {
				_log.warn(
					"Blog entry '" + title +
						"' could not be created, skipping: " +
							e.getMessage());

				skipped++;
			}

			progress.onProgress(i + 1, count);
		}

		int createdCount = created.size();

		if (createdCount == count) {
			return BatchResult.success(count, created, skipped);
		}

		String errorMessage;

		if (createdCount == 0) {
			errorMessage = "No blog entries were created (all attempts failed)";
		}
		else {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" blog entries were created; " + skipped +
						" skipped due to errors.";
		}

		return BatchResult.failure(count, created, skipped, errorMessage);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BlogsCreator.class);

	@Reference
	private BlogsEntryLocalService _blogsEntryLocalService;

}
