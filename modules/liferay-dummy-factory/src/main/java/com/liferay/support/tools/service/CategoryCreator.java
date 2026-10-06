package com.liferay.support.tools.service;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CategoryCreator.class)
public class CategoryCreator {

	public BatchResult<AssetCategory> create(
			long userId, long groupId, long vocabularyId, BatchSpec batchSpec,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);

		List<AssetCategory> categories = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(
				batchSpec.baseName(), count, i, " ");

			Map<Locale, String> titleMap = Collections.singletonMap(
				LocaleUtil.getDefault(), name);

			categories.add(
				BatchTransaction.run(
					() -> _assetCategoryLocalService.addCategory(
						null, userId, groupId, 0L, titleMap,
						Collections.emptyMap(), vocabularyId, new String[0],
						serviceContext)));

			progress.onProgress(i + 1, count);
		}

		return BatchResult.success(count, categories, 0);
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

}
