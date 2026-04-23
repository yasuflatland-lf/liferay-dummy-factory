package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;

import java.util.function.Function;

class ResourceCommandUtil {

	static BatchSpec parseBatchSpec(JSONObject data) {
		return new BatchSpec(
			GetterUtil.getInteger(data.getString("count")),
			data.getString("baseName"));
	}

	static void setErrorResponse(JSONObject responseJson, Throwable throwable) {
		String message = throwable.getMessage();

		responseJson.put(
			"error",
			(message != null) ? message : "An unexpected error occurred");
		responseJson.put("success", false);
	}

	static <T> JSONObject toJson(
		BatchResult<T> result, Function<T, JSONObject> itemMapper) {

		JSONArray itemsArray = JSONFactoryUtil.createJSONArray();

		for (T item : result.items()) {
			itemsArray.put(itemMapper.apply(item));
		}

		JSONObject json = JSONFactoryUtil.createJSONObject();

		json.put("count", result.count());
		json.put("items", itemsArray);
		json.put("requested", result.requested());
		json.put("skipped", result.skipped());
		json.put("success", result.success());

		if (!result.success()) {
			json.put("error", result.error());
		}

		return json;
	}

	static void validateCount(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("count must be greater than 0");
		}
	}

	static void validateNotEmpty(String value, String fieldName) {
		if ((value == null) || value.isEmpty()) {
			throw new IllegalArgumentException(
				fieldName + " must not be empty");
		}
	}

	static void validatePositiveId(long id, String fieldName) {
		if (id <= 0) {
			throw new IllegalArgumentException(
				fieldName + " must be greater than 0");
		}
	}

}
