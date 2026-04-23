package com.liferay.support.tools.service.datalist;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.message.boards.service.MBCategoryLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class MBCategoriesDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		return JSONFactoryUtil.createJSONArray();
	}

	@Override
	public JSONArray getOptions(
			long companyId, String type,
			HttpServletRequest httpServletRequest)
		throws Exception {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long groupId = ParamUtil.getLong(httpServletRequest, "groupId");

		List<MBCategory> categories;

		if (groupId > 0) {
			categories = _mbCategoryLocalService.getCategories(groupId);
		}
		else {
			categories = _mbCategoryLocalService.getCompanyCategories(
				companyId, QueryUtil.ALL_POS, QueryUtil.ALL_POS);
		}

		for (MBCategory category : categories) {
			jsonArray.put(
				createOption(category.getName(), category.getCategoryId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"mb-categories"};
	}

	@Reference
	private MBCategoryLocalService _mbCategoryLocalService;

}
