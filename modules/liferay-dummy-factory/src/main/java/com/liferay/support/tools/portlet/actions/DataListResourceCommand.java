package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.DataListProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/data"
	},
	service = MVCResourceCommand.class
)
public class DataListResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		String type = ParamUtil.getString(httpServletRequest, "type");

		long companyId = _portal.getCompanyId(httpServletRequest);

		DataListProvider provider = _providers.get(type);

		if (provider == null) {
			_log.error("Unknown data list type requested: " + type);

			JSONObject errorResponse = JSONFactoryUtil.createJSONObject();

			errorResponse.put("error", "Unknown data list type: " + type);

			JSONPortletResponseUtil.writeJSON(
				resourceRequest, resourceResponse, errorResponse);

			return;
		}

		try {
			JSONArray jsonArray = provider.getOptions(
				companyId, type, httpServletRequest);

			JSONPortletResponseUtil.writeJSON(
				resourceRequest, resourceResponse, jsonArray);
		}
		catch (Exception exception) {
			_log.error(
				"Failed to load data list options for type: " + type,
				exception);

			JSONObject errorResponse = JSONFactoryUtil.createJSONObject();

			errorResponse.put(
				"error",
				"Failed to load options for type: " + type);

			JSONPortletResponseUtil.writeJSON(
				resourceRequest, resourceResponse, errorResponse);
		}
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		unbind = "_removeProvider"
	)
	private void _addProvider(DataListProvider provider) {
		for (String type : provider.getSupportedTypes()) {
			_providers.put(type, provider);
		}
	}

	private void _removeProvider(DataListProvider provider) {
		for (String type : provider.getSupportedTypes()) {
			_providers.remove(type, provider);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DataListResourceCommand.class);

	@Reference
	private Portal _portal;

	private final Map<String, DataListProvider> _providers =
		new ConcurrentHashMap<>();

}
