package com.liferay.support.tools.service.datalist;

import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class DLFolderDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		jsonArray.put(createOption("Root Folder", 0));

		return jsonArray;
	}

	@Override
	public JSONArray getOptions(
		long companyId, String type,
		HttpServletRequest httpServletRequest) {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		jsonArray.put(createOption("Root Folder", 0));

		long groupId = ParamUtil.getLong(httpServletRequest, "groupId", 0);

		if (groupId <= 0) {
			return jsonArray;
		}

		try {
			List<DLFolder> dlFolders = _dlFolderLocalService.getFolders(
				groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
				QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

			for (DLFolder dlFolder : dlFolders) {
				jsonArray.put(
					createOption(dlFolder.getName(), dlFolder.getFolderId()));
			}
		}
		catch (Exception exception) {
			_log.error(
				"Failed to load folders for group " + groupId, exception);
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"folders"};
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DLFolderDataListProvider.class);

	@Reference
	private DLFolderLocalService _dlFolderLocalService;

}
