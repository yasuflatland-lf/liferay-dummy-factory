package com.liferay.support.tools.service;

import com.liferay.portal.kernel.exception.DuplicateOrganizationException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = OrganizationCreator.class)
public class OrganizationCreator {

	public BatchResult<Organization> create(
			long userId, BatchSpec batchSpec,
			long parentOrganizationId, boolean site,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		List<Organization> created = new ArrayList<>(count);
		int skipped = 0;

		for (int i = 0; i < count; i++) {
			final String name = BatchNaming.resolve(baseName, count, i, " ");

			try {
				Organization organization = BatchTransaction.run(
					() -> _organizationLocalService.addOrganization(
						userId, parentOrganizationId, name, site));

				created.add(organization);
			}
			catch (DuplicateOrganizationException e) {
				_log.warn(
					"Organization '" + name +
						"' already exists, skipping");

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
				"No organizations were created (all names may " +
					"already exist)";
		}
		else if (skipped > 0) {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" organizations were created; " + skipped +
						" skipped because the name already existed.";
		}
		else {
			errorMessage =
				"Only " + createdCount + " of " + count +
					" organizations were created.";
		}

		return BatchResult.failure(count, created, skipped, errorMessage);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OrganizationCreator.class);

	@Reference
	private OrganizationLocalService _organizationLocalService;

}
