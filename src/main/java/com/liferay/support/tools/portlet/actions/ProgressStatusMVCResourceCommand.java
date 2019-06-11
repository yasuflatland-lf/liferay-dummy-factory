package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ProgressTracker;
import com.liferay.portal.kernel.util.ProgressTrackerThreadLocal;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Progress Tracker resource command
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"javax.portlet.name=" + LDFPortletKeys.PORTLET_CONFIGURATION,
		"mvc.command.name=/ldf/progress/status"
	},
	service = MVCResourceCommand.class
)
public class ProgressStatusMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws Exception {
		HttpServletResponse response = _portal.getHttpServletResponse(resourceResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		String serializedJson = createReturnJson(resourceRequest, resourceResponse);
		
		ServletResponseUtil.write(response, serializedJson);		

	}
	
	/**
	 * Create Return JSON
	 * 
	 * @param resourceRequest
	 * @param resourceResponse
	 * @return
	 */
	protected String createReturnJson(ResourceRequest resourceRequest, ResourceResponse resourceResponse) {

		JSONObject rootJSONObject = JSONFactoryUtil.createJSONObject();
		JSONObject percentageObject = JSONFactoryUtil.createJSONObject();

		ProgressTracker progressTracker =
				ProgressTrackerThreadLocal.getProgressTracker();

		percentageObject.put("percentage", progressTracker.getPercent());

		rootJSONObject.put("data", percentageObject);

		return rootJSONObject.toJSONString();
	}
	
	@Reference
	private Portal _portal;
	
}
