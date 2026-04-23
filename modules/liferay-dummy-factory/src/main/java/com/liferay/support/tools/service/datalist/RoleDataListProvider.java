package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class RoleDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		int roleType;

		switch (type) {
			case "site-roles":
				roleType = RoleConstants.TYPE_SITE;

				break;
			case "org-roles":
				roleType = RoleConstants.TYPE_ORGANIZATION;

				break;
			default:
				roleType = RoleConstants.TYPE_REGULAR;

				break;
		}

		List<Role> roles = _roleLocalService.getRoles(
			companyId, new int[] {roleType});

		for (Role role : roles) {
			jsonArray.put(createOption(role.getName(), role.getRoleId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"roles", "site-roles", "org-roles"};
	}

	@Reference
	private RoleLocalService _roleLocalService;

}
