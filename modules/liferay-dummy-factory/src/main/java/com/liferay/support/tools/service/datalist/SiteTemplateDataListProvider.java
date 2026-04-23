package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.LayoutSetPrototype;
import com.liferay.portal.kernel.service.LayoutSetPrototypeLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class SiteTemplateDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<LayoutSetPrototype> layoutSetPrototypes =
			_layoutSetPrototypeLocalService.getLayoutSetPrototypes(
				companyId);

		for (LayoutSetPrototype prototype : layoutSetPrototypes) {
			if (prototype.isActive()) {
				jsonArray.put(
					createOption(
						prototype.getName(LocaleUtil.getDefault()),
						prototype.getLayoutSetPrototypeId()));
			}
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"site-templates"};
	}

	@Reference
	private LayoutSetPrototypeLocalService _layoutSetPrototypeLocalService;

}
