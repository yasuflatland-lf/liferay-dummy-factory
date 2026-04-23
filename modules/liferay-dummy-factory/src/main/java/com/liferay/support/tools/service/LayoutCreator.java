package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.LayoutFriendlyURLException;
import com.liferay.portal.kernel.exception.LayoutNameException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = LayoutCreator.class)
public class LayoutCreator {

	public BatchResult<Layout> create(
			long userId, BatchSpec batchSpec, long groupId, String type,
			boolean privateLayout, boolean hidden,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		List<Layout> layouts = new ArrayList<>(count);
		int skipped = 0;

		for (int i = 0; i < count; i++) {
			final String name = BatchNaming.resolve(baseName, count, i, " ");

			try {
				Layout layout = _invokeAddLayout(
					userId, groupId, privateLayout, name, type, hidden);

				layouts.add(layout);
			}
			catch (LayoutFriendlyURLException | LayoutNameException e) {
				_log.warn(
					"Layout '" + name + "' could not be created: " +
						e.getMessage(),
					e);

				skipped++;
			}

			progress.onProgress(i + 1, count);
		}

		int createdCount = layouts.size();
		boolean success = (createdCount == count);

		if (success) {
			return BatchResult.success(count, layouts, skipped);
		}

		String errorMessage;

		if (createdCount == 0) {
			errorMessage =
				"No pages were created (all names may be invalid or " +
					"already exist)";
		}
		else if (skipped > 0) {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" pages were created; " + skipped +
						" skipped because the name was invalid or " +
							"already existed.";
		}
		else {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" pages were created.";
		}

		return BatchResult.failure(count, layouts, skipped, errorMessage);
	}

	private Layout _invokeAddLayout(
			long userId, long groupId, boolean privateLayout, String name,
			String type, boolean hidden)
		throws Throwable {

		return BatchTransaction.run(
			() -> _layoutLocalService.addLayout(
				StringPool.BLANK, userId, groupId, privateLayout,
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, name,
				StringPool.BLANK, StringPool.BLANK, type, hidden,
				StringPool.BLANK, new ServiceContext()));
	}

	private static final Log _log = LogFactoryUtil.getLog(LayoutCreator.class);

	@Reference
	private LayoutLocalService _layoutLocalService;

}
