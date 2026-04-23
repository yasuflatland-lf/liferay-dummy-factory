package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.LayoutSetPrototype;
import com.liferay.portal.kernel.service.LayoutSetPrototypeLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class LayoutSetPrototypeDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		jsonArray.put(createOption("none", 0));

		try {
			List<LayoutSetPrototype> layoutSetPrototypes =
				_layoutSetPrototypeLocalService.search(
					companyId, Boolean.TRUE, QueryUtil.ALL_POS,
					QueryUtil.ALL_POS, null);

			for (LayoutSetPrototype layoutSetPrototype : layoutSetPrototypes) {
				jsonArray.put(
					createOption(
						layoutSetPrototype.getName(LocaleUtil.getDefault()),
						layoutSetPrototype.getLayoutSetPrototypeId()));
			}
		}
		catch (Exception exception) {
			_log.error(
				"Failed to load layout set prototypes for company " +
					companyId,
				exception);
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"layout-set-prototypes"};
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LayoutSetPrototypeDataListProvider.class);

	@Reference
	private LayoutSetPrototypeLocalService _layoutSetPrototypeLocalService;

}
