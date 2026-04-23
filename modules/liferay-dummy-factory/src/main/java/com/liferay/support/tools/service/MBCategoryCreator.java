package com.liferay.support.tools.service;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.message.boards.service.MBCategoryLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MBCategoryCreator.class)
public class MBCategoryCreator {

	public BatchResult<MBCategory> create(
			long userId, long groupId, BatchSpec batchSpec, String description,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		List<MBCategory> categories = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(
				batchSpec.baseName(), count, i, " ");

			categories.add(
				BatchTransaction.run(
					() -> _mbCategoryLocalService.addCategory(
						null, userId, 0L, name, description, serviceContext)));

			progress.onProgress(i + 1, count);
		}

		return BatchResult.success(count, categories, 0);
	}

	@Reference
	private MBCategoryLocalService _mbCategoryLocalService;

}
