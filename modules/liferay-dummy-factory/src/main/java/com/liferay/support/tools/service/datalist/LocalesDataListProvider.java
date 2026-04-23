package com.liferay.support.tools.service.datalist;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.Locale;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

@Component(service = DataListProvider.class)
public class LocalesDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		Set<Locale> locales = LanguageUtil.getAvailableLocales();

		for (Locale locale : locales) {
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

			jsonObject.put("label", locale.getDisplayName());
			jsonObject.put("value", LocaleUtil.toLanguageId(locale));

			jsonArray.put(jsonObject);
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"locales"};
	}

}
