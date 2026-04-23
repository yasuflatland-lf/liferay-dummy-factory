package com.liferay.support.tools.portlet.actions;

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.TempFileEntryUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.io.InputStream;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/doc/upload"
	},
	service = MVCResourceCommand.class
)
public class DocumentUploadResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		UploadPortletRequest uploadPortletRequest =
			_portal.getUploadPortletRequest(resourceRequest);

		String cmd = ParamUtil.getString(uploadPortletRequest, "cmd");

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		try {
			if ("add_temp".equals(cmd)) {
				String sourceFileName = uploadPortletRequest.getFileName(
					"file");

				if ((sourceFileName == null) || sourceFileName.isEmpty()) {
					responseJson.put("error", "file is required");
					responseJson.put("success", false);
				}
				else {
					long groupId = ParamUtil.getLong(
						uploadPortletRequest, "groupId");

					if (groupId <= 0) {
						responseJson.put("success", false);
						responseJson.put("error", "groupId is required");
						JSONPortletResponseUtil.writeJSON(
							resourceRequest, resourceResponse, responseJson);
						return;
					}

					InputStream inputStream =
						uploadPortletRequest.getFileAsStream("file");
					String mimeType = uploadPortletRequest.getContentType(
						"file");

					String tempFileName = TempFileEntryUtil.getTempFileName(
						sourceFileName);

					FileEntry fileEntry = _dlAppService.addTempFileEntry(
						groupId, 0, LDFPortletKeys.DOCUMENT_TEMP_FOLDER_NAME, tempFileName,
						inputStream, mimeType);

					responseJson.put("fileName", fileEntry.getTitle());
					responseJson.put("originalName", sourceFileName);
					responseJson.put("success", true);
				}
			}
			else if ("delete_temp".equals(cmd)) {
				String fileName = ParamUtil.getString(
					uploadPortletRequest, "fileName");

				long groupId = ParamUtil.getLong(
					uploadPortletRequest, "groupId");

				if (groupId <= 0) {
					responseJson.put("success", false);
					responseJson.put("error", "groupId is required");
					JSONPortletResponseUtil.writeJSON(
						resourceRequest, resourceResponse, responseJson);
					return;
				}

				_dlAppService.deleteTempFileEntry(
					groupId, 0, LDFPortletKeys.DOCUMENT_TEMP_FOLDER_NAME, fileName);

				responseJson.put("deleted", true);
				responseJson.put("success", true);
			}
			else {
				responseJson.put("error", "unknown cmd: " + cmd);
				responseJson.put("success", false);
			}
		}
		catch (Exception exception) {
			_log.error(
				"Temp file operation failed (cmd=" + cmd + ")", exception);

			ResourceCommandUtil.setErrorResponse(responseJson, exception);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DocumentUploadResourceCommand.class);

	@Reference
	private DLAppService _dlAppService;

	@Reference
	private Portal _portal;

}
