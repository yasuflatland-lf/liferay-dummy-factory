package com.liferay.support.tools.service;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public interface DataListProvider {

	default JSONObject createOption(String label, long value) {
		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("label", label);
		jsonObject.put("value", String.valueOf(value));

		return jsonObject;
	}

	JSONArray getOptions(long companyId, String type) throws Exception;

	default JSONArray getOptions(
			long companyId, String type,
			HttpServletRequest httpServletRequest)
		throws Exception {

		return getOptions(companyId, type);
	}

	String[] getSupportedTypes();

}
