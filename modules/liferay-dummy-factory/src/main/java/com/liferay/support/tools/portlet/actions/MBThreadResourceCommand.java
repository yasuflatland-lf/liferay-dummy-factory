package com.liferay.support.tools.portlet.actions;

import com.liferay.message.boards.model.MBMessage;
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
import com.liferay.support.tools.service.MBThreadCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/mb-thread"
	},
	service = MVCResourceCommand.class
)
public class MBThreadResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create MB threads",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));
				long categoryId = GetterUtil.getLong(
					data.getString("categoryId"));

				String body = GetterUtil.getString(
					data.getString("body"), "This is a test message.");
				String format = GetterUtil.getString(
					data.getString("format"), "html");

				ResourceCommandUtil.validatePositiveId(groupId, "groupId");

				if (categoryId < 0) {
					throw new IllegalArgumentException(
						"categoryId must be greater than or equal to 0");
				}

				BatchResult<MBMessage> result = _mbThreadCreator.create(
					context.getUserId(), groupId, categoryId, batchSpec, body,
					format, context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					message -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("categoryId", message.getCategoryId());
						json.put("groupId", message.getGroupId());
						json.put("messageId", message.getMessageId());
						json.put("subject", message.getSubject());
						json.put("threadId", message.getThreadId());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MBThreadResourceCommand.class);

	@Reference
	private MBThreadCreator _mbThreadCreator;

	@Reference
	private Portal _portal;

}
