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
import com.liferay.support.tools.service.MBReplyCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/mb-reply"
	},
	service = MVCResourceCommand.class
)
public class MBReplyResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create MB replies",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long threadId = GetterUtil.getLong(data.getString("threadId"));

				String body = GetterUtil.getString(
					data.getString("body"), "This is a test reply.");
				String format = GetterUtil.getString(
					data.getString("format"), "html");

				ResourceCommandUtil.validatePositiveId(threadId, "threadId");

				BatchResult<MBMessage> result = _mbReplyCreator.create(
					context.getUserId(), threadId, batchSpec, body, format,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					reply -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("body", reply.getBody());
						json.put("messageId", reply.getMessageId());
						json.put("subject", reply.getSubject());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MBReplyResourceCommand.class);

	@Reference
	private MBReplyCreator _mbReplyCreator;

	@Reference
	private Portal _portal;

}
