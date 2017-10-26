package com.liferay.support.tools.document.library;

import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class DLContext extends ParamContext {
	
	private long numberOfDocuments = 0;
	private String baseDocumentTitle = "";
	private String baseDocumentDescription = "";
	private long groupId = 0;
	private long folderId = 0;  
	
	public DLContext(ActionRequest actionRequest) {
		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		// Fetch data
		numberOfDocuments = ParamUtil.getLong(actionRequest, "numberOfDocuments", 1);
		baseDocumentTitle = ParamUtil.getString(actionRequest, "baseDocumentTitle", "");
		baseDocumentDescription = ParamUtil.getString(actionRequest, "baseDocumentDescription", "");
		folderId = ParamUtil.getLong(actionRequest, "folderId", 0);
		
		// Sites
		groupId = ParamUtil.getLong(actionRequest, "groupId", themeDisplay.getScopeGroupId());

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
