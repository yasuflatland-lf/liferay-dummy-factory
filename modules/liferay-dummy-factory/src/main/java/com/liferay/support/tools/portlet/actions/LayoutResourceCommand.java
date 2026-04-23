package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.LayoutCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/page"
	},
	service = MVCResourceCommand.class
)
public class LayoutResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create layouts",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));

				ResourceCommandUtil.validatePositiveId(groupId, "groupId");

				String type = GetterUtil.getString(
					data.getString("type"), "portlet");
				boolean privateLayout = GetterUtil.getBoolean(
					data.getString("privateLayout"));
				boolean hidden = GetterUtil.getBoolean(
					data.getString("hidden"));

				BatchResult<Layout> result = _layoutCreator.create(
					context.getUserId(), batchSpec, groupId, type,
					privateLayout, hidden, context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					layout -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("friendlyURL", layout.getFriendlyURL());
						json.put("layoutId", layout.getLayoutId());
						json.put(
							"name",
							layout.getName(LocaleUtil.getSiteDefault()));
						json.put("plid", layout.getPlid());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LayoutResourceCommand.class);

	@Reference
	private LayoutCreator _layoutCreator;

	@Reference
	private Portal _portal;

}
