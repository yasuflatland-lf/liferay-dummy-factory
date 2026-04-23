package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CompanyCreator.class)
public class CompanyCreator {

	public BatchResult<Company> create(
			int count, String webId, String virtualHostname, String mx,
			int maxUsers, boolean active, ProgressCallback progress)
		throws Throwable {

		List<Company> companies = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String prefix = (count > 1) ? String.valueOf(i + 1) : "";

			companies.add(
				BatchTransaction.run(
					() -> _companyLocalService.addCompany(
						null, prefix + webId, prefix + virtualHostname,
						prefix + mx, maxUsers, active, false, null, null, null,
						null, null, null)));

			progress.onProgress(i + 1, count);
		}

		return BatchResult.success(count, companies, 0);
	}

	@Reference
	private CompanyLocalService _companyLocalService;

}
