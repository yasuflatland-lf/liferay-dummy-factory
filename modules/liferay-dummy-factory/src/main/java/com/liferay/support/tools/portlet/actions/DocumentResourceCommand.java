package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.DocumentCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/doc"
	},
	service = MVCResourceCommand.class
)
public class DocumentResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create documents",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));

				if (groupId <= 0) {
					throw new IllegalArgumentException("site is required");
				}

				long folderId = GetterUtil.getLong(
					data.getString("folderId"), 0L);
				String description = GetterUtil.getString(
					data.getString("description"), "");
				String uploadedFilesStr = data.getString("uploadedFiles");

				String[] uploadedFiles =
					Validator.isNotNull(uploadedFilesStr) ?
						uploadedFilesStr.split(",") : new String[0];

				BatchResult<FileEntry> result = _documentCreator.create(
					context.getUserId(), groupId, batchSpec, folderId,
					description, uploadedFiles, context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					fileEntry -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("fileEntryId", fileEntry.getFileEntryId());
						json.put("title", fileEntry.getTitle());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DocumentResourceCommand.class);

	@Reference
	private DocumentCreator _documentCreator;

	@Reference
	private Portal _portal;

}
