package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.utils.ProgressManager;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import jakarta.servlet.http.HttpServletRequest;

class PortletJsonCommandTemplate {

	static void serveJsonWithProgress(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse,
			Portal portal, Log log, String errorLogMessage,
			JsonCommandHandler jsonCommandHandler)
		throws Exception {

		HttpServletRequest httpServletRequest =
			portal.getOriginalServletRequest(
				portal.getHttpServletRequest(resourceRequest));

		String dataString = ParamUtil.getString(httpServletRequest, "data");

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		ProgressManager progressManager = new ProgressManager();

		boolean progressStarted = false;

		try {
			progressManager.start(resourceRequest);
			progressStarted = true;

			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			JSONObject handledResponseJson = jsonCommandHandler.handle(
				new PortletJsonCommandContext(
					resourceRequest, portal, progressManager),
				data, responseJson);

			if (handledResponseJson != null) {
				responseJson = handledResponseJson;
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			log.error(errorLogMessage, throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}
		finally {
			if (progressStarted) {
				progressManager.finish();
			}
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	@FunctionalInterface
	interface JsonCommandHandler {

		JSONObject handle(
				PortletJsonCommandContext context, JSONObject data,
				JSONObject responseJson)
			throws Throwable;

	}

	static class PortletJsonCommandContext {

		ProgressCallback getProgressCallback() {
			return ProgressCallback.fromProgressManager(_progressManager);
		}

		long getUserId()
			throws Exception {

			return _portal.getUserId(_resourceRequest);
		}

		long getCompanyId()
			throws Exception {

			long companyId = _portal.getCompanyId(_resourceRequest);

			if (companyId <= 0) {
				throw new IllegalArgumentException(
					"Could not resolve company ID for request");
			}

			return companyId;
		}

		private PortletJsonCommandContext(
			ResourceRequest resourceRequest, Portal portal,
			ProgressManager progressManager) {

			_portal = portal;
			_progressManager = progressManager;
			_resourceRequest = resourceRequest;
		}

		private final Portal _portal;
		private final ProgressManager _progressManager;
		private final ResourceRequest _resourceRequest;

	}

}
