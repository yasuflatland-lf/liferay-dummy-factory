package com.liferay.support.tools.portlet.actions;

import com.liferay.blogs.model.BlogsEntry;
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
import com.liferay.support.tools.service.BlogsBatchSpec;
import com.liferay.support.tools.service.BlogsCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.BLOGS
	},
	service = MVCResourceCommand.class
)
public class BlogsResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create blog entries",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));

				ResourceCommandUtil.validatePositiveId(groupId, "groupId");

				String content = data.getString("content");
				String subtitle = data.getString("subtitle");
				String description = data.getString("description");
				boolean allowPingbacks = GetterUtil.getBoolean(
					data.getString("allowPingbacks"));
				boolean allowTrackbacks = GetterUtil.getBoolean(
					data.getString("allowTrackbacks"));

				BlogsBatchSpec blogsBatchSpec = new BlogsBatchSpec(
					batchSpec, groupId, content, subtitle, description,
					allowPingbacks, allowTrackbacks, null);

				long userId = GetterUtil.getLong(
					data.getString("userId"), context.getUserId());

				BatchResult<BlogsEntry> result = _blogsCreator.create(
					userId, blogsBatchSpec, context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					entry -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("entryId", entry.getEntryId());
						json.put("title", entry.getTitle());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BlogsResourceCommand.class);

	@Reference
	private BlogsCreator _blogsCreator;

	@Reference
	private Portal _portal;

}
