package com.liferay.support.tools.page;

import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class PageContext extends ParamContext {
	
	private long numberOfpages = 0;
	private String basePageName = "";
	private long groupId = 0;
	private long parentLayoutId;
	private String layoutType;
	private boolean privateLayout;
	private boolean hidden;
	
	public PageContext(ActionRequest actionRequest) {
		//Fetch data
		numberOfpages = ParamUtil.getLong(actionRequest, "numberOfpages",0);
		basePageName = ParamUtil.getString(actionRequest, "basePageName","");
		groupId = ParamUtil.getLong(actionRequest, "group",0);
		parentLayoutId = ParamUtil.getLong(actionRequest, "parentLayoutId",LayoutConstants.DEFAULT_PARENT_LAYOUT_ID);
		layoutType = ParamUtil.getString(actionRequest, "layoutType",LayoutConstants.TYPE_PORTLET);
		privateLayout = ParamUtil.getBoolean(actionRequest, "privateLayout", false);
		hidden = ParamUtil.getBoolean(actionRequest, "hidden", false);
	}

	public long getNumberOfpages() {
		return numberOfpages;
	}

	public void setNumberOfpages(long numberOfpages) {
		this.numberOfpages = numberOfpages;
	}

	public String getBasePageName() {
		return basePageName;
	}

	public void setBasePageName(String basePageName) {
		this.basePageName = basePageName;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public long getParentLayoutId() {
		return parentLayoutId;
	}

	public void setParentLayoutId(long parentLayoutId) {
		this.parentLayoutId = parentLayoutId;
	}

	public String getLayoutType() {
		return layoutType;
	}

	public void setLayoutType(String layoutType) {
		this.layoutType = layoutType;
	}

	public boolean isPrivateLayout() {
		return privateLayout;
	}

	public void setPrivateLayout(boolean privateLayout) {
		this.privateLayout = privateLayout;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
}
