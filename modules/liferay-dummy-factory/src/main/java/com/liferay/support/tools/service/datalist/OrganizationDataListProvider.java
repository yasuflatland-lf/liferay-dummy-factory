package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class OrganizationDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<Organization> organizations =
			_organizationLocalService.getOrganizations(
				companyId,
				OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID,
				QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		for (Organization organization : organizations) {
			jsonArray.put(
				createOption(
					organization.getName(),
					organization.getOrganizationId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"organizations"};
	}

	@Reference
	private OrganizationLocalService _organizationLocalService;

}
