package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class UsersDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<User> users = _userLocalService.getCompanyUsers(
			companyId, QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		for (User user : users) {
			if (user.isDefaultUser()) {
				continue;
			}

			String label =
				user.getScreenName() + " (" + user.getEmailAddress() + ")";

			jsonArray.put(createOption(label, user.getUserId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"users"};
	}

	@Reference
	private UserLocalService _userLocalService;

}
