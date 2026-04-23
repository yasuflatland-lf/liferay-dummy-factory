package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.ProgressTracker;
import com.liferay.support.tools.constants.LDFPortletKeys;

import jakarta.portlet.PortletSession;
import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/progress"
	},
	service = MVCResourceCommand.class
)
public class ProgressResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		try {
			String progressId = ParamUtil.getString(
				resourceRequest, _PROGRESS_ID_PARAM, _DEFAULT_PROGRESS_ID);

			int percent = _readPercent(resourceRequest, progressId);

			responseJson.put("percent", percent);
		}
		catch (Throwable throwable) {
			_log.error("Failed to read progress", throwable);

			responseJson.put("percent", 0);
			responseJson.put("error", throwable.getMessage());
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private int _readPercent(
		ResourceRequest resourceRequest, String progressId) {

		PortletSession portletSession = resourceRequest.getPortletSession(
			false);

		if (portletSession == null) {
			return 0;
		}

		Object attribute = portletSession.getAttribute(
			ProgressTracker.PERCENT + progressId,
			PortletSession.APPLICATION_SCOPE);

		if (attribute instanceof ProgressTracker) {
			return ((ProgressTracker)attribute).getPercent();
		}

		return 0;
	}

	private static final String _DEFAULT_PROGRESS_ID = "COMMON_PROGRESS_ID";

	private static final String _PROGRESS_ID_PARAM = "progressId";

	private static final Log _log = LogFactoryUtil.getLog(
		ProgressResourceCommand.class);

}
