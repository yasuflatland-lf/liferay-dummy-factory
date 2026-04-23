package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class SiteDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<Group> groups = _groupLocalService.getGroups(
			companyId, GroupConstants.DEFAULT_PARENT_GROUP_ID, true);

		for (Group group : groups) {
			try {
				jsonArray.put(
					createOption(
						group.getDescriptiveName(), group.getGroupId()));
			}
			catch (PortalException portalException) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						"Unable to get descriptive name for group " +
							group.getGroupId() +
							", using name as fallback",
						portalException);
				}

				jsonArray.put(
					createOption(group.getName(), group.getGroupId()));
			}
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"sites"};
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteDataListProvider.class);

	@Reference
	private GroupLocalService _groupLocalService;

}
