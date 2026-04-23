package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.DuplicateRoleException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.service.RoleLocalService;
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

@Component(service = RoleCreator.class)
public class RoleCreator {

	public BatchResult<Role> create(
			long userId, BatchSpec batchSpec,
			RoleType roleType, String description,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		int type = roleType.toLiferayConstant();

		List<Role> created = new ArrayList<>(count);
		int skipped = 0;

		Map<Locale, String> descriptionMap = Collections.singletonMap(
			LocaleUtil.getDefault(), description);

		for (int i = 0; i < count; i++) {
			final String name = BatchNaming.resolve(baseName, count, i);

			final Map<Locale, String> titleMap = Collections.singletonMap(
				LocaleUtil.getDefault(), name);

			try {
				Role role = BatchTransaction.run(
					() -> _roleLocalService.addRole(
						StringPool.BLANK, userId, null, 0, name, titleMap,
						descriptionMap, type, null, null));

				created.add(role);
			}
			catch (DuplicateRoleException e) {
				_log.warn(
					"Role '" + name + "' already exists, skipping");

				skipped++;
			}

			progress.onProgress(i + 1, count);
		}

		int createdCount = created.size();
		boolean success = (createdCount == count);

		if (success) {
			return BatchResult.success(count, created, skipped);
		}

		String errorMessage;

		if (createdCount == 0) {
			errorMessage =
				"No roles were created (all names may already exist)";
		}
		else if (skipped > 0) {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" roles were created; " + skipped +
						" skipped because the name already existed.";
		}
		else {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" roles were created.";
		}

		return BatchResult.failure(count, created, skipped, errorMessage);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RoleCreator.class);

	@Reference
	private RoleLocalService _roleLocalService;

}
