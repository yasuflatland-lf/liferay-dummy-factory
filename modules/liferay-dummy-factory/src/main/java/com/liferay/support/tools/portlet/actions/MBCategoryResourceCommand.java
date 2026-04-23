package com.liferay.support.tools.portlet.actions;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBCategoryCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/mb-category"
	},
	service = MVCResourceCommand.class
)
public class MBCategoryResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create MB categories",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));

				ResourceCommandUtil.validatePositiveId(groupId, "groupId");

				String description = GetterUtil.getString(
					data.getString("description"));

				BatchResult<MBCategory> result = _mbCategoryCreator.create(
					context.getUserId(), groupId, batchSpec, description,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					category -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("categoryId", category.getCategoryId());
						json.put("groupId", category.getGroupId());
						json.put("name", category.getName());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MBCategoryResourceCommand.class);

	@Reference
	private MBCategoryCreator _mbCategoryCreator;

	@Reference
	private Portal _portal;

}
