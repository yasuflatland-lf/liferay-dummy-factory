package com.liferay.support.tools.document.library;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.TempFileEntryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.common.ParamContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.portlet.ActionRequest;

public class DLContext extends ParamContext {

	private long numberOfDocuments = 0;
	private String baseDocumentTitle = "";
	private String baseDocumentDescription = "";
	private String baseDocumentFields = "";
	private long groupId = 0;
	private long fileEntryTypeId = 0;
	private long folderId = 0;
	private List<String> tempFileNames = new ArrayList<>();
	private List<FileEntry> tempFileEntries = new ArrayList<>();

	public DLContext(ActionRequest actionRequest) {
		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		// Fetch data
		numberOfDocuments = ParamUtil.getLong(actionRequest, "numberOfDocuments", 1);
		baseDocumentTitle = ParamUtil.getString(actionRequest, "baseDocumentTitle", "");
		baseDocumentDescription = ParamUtil.getString(actionRequest, "baseDocumentDescription", "");
		baseDocumentFields = ParamUtil.getString(actionRequest, "baseDocumentFields", "");
		folderId = ParamUtil.getLong(actionRequest, "folderId", 0);

		String[] tempFiles =
				ParamUtil.getParameterValues(
					actionRequest,
					"selectUploadedFile",
					new String[0],
					false
				);

		if(!Validator.isNull(tempFiles)) {
			tempFileNames.addAll(Arrays.asList(tempFiles));
			tempFileEntries = setTempFileEntries(
					themeDisplay.getScopeGroupId(), themeDisplay.getUserId(), tempFileNames);
		}

		// Sites
		groupId = ParamUtil.getLong(actionRequest, "groupId", themeDisplay.getScopeGroupId());
		fileEntryTypeId = ParamUtil.getLong(actionRequest, "fileEntryTypeId", themeDisplay.getScopeGroupId());

	}

	/**
	 * Get Random Temp File Entry
	 *
	 * @return random temp file entry
	 */
	public FileEntry getRandomFileEntry() {
		List<FileEntry> tmpObj = new ArrayList<>();
		tmpObj.addAll(tempFileEntries);
		Collections.shuffle(tmpObj);
		return tmpObj.get(0);
	}

	public String getBaseDocumentFields(){
		return baseDocumentFields;
	}

	public List<FileEntry> getTempFileEntries() {
		return tempFileEntries;
	}

	/**
	 * Set Temp file
	 * @param selectUploadedFiles
	 * @return
	 * @throws PortalException
	 */
	protected List<FileEntry> setTempFileEntries(long groupId, long userId, List<String> selectUploadedFiles) {
		List<FileEntry> tempFiles = new ArrayList<>();

		for(String selectedFileName  : selectUploadedFiles ) {
			FileEntry tempFileEntry;
			try {
				tempFileEntry = TempFileEntryUtil.getTempFileEntry(
					groupId,
					userId,
					EditFileEntryMVCActionCommand.TEMP_FOLDER_NAME,
					selectedFileName
				);

				tempFiles.add(tempFileEntry);

			} catch (PortalException e) {
				e.printStackTrace();
				return new ArrayList<>();
			}
		}

		return tempFiles;
	}

	public List<String> getSelectUploadedFiles() {
		return tempFileNames;
	}

	public void setSelectUploadedFiles(List<String> selectUploadedFiles) {
		this.tempFileNames = selectUploadedFiles;
	}

	public long getNumberOfDocuments() {
		return numberOfDocuments;
	}

	public void setNumberOfDocuments(long numberOfDocuments) {
		this.numberOfDocuments = numberOfDocuments;
	}

	public String getBaseDocumentTitle() {
		return baseDocumentTitle;
	}

	public void setBaseDocumentTitle(String baseDocumentTitle) {
		this.baseDocumentTitle = baseDocumentTitle;
	}

	public String getBaseDocumentDescription() {
		return baseDocumentDescription;
	}

	public void setBaseDocumentDescription(String baseDocumentDescription) {
		this.baseDocumentDescription = baseDocumentDescription;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public long getFolderId() {
		return folderId;
	}

	public void setFolderId(long folderId) {
		this.folderId = folderId;
	}

}
