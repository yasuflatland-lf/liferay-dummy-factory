package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class UserGroupDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<UserGroup> userGroups =
			_userGroupLocalService.getUserGroups(companyId);

		for (UserGroup userGroup : userGroups) {
			jsonArray.put(
				createOption(
					userGroup.getName(), userGroup.getUserGroupId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"user-groups"};
	}

	@Reference
	private UserGroupLocalService _userGroupLocalService;

}
