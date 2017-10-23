package com.liferay.support.tools.messageboard;

import com.liferay.message.boards.kernel.model.MBCategoryConstants;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.common.ParamContext;
import com.liferay.support.tools.utils.CommonUtil;

import javax.portlet.ActionRequest;

public class MBContext extends ParamContext {
	
	private long numberOfMB = 0;
	private long[] groupIds;
	private long categoryId;
	private String subject = StringPool.BLANK;
	private String body = StringPool.BLANK;
	private boolean anonymous = false;
	private boolean allowPingbacks = false;
	private double priority = 0.0;	
	private long parentCategoryId = 0;
	private String categoryName = "";
	private String description = "";
	private long threadId = 0;
	private long siteGroupId = 0;
	
	public MBContext(ActionRequest actionRequest) {
		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		//Fetch data
		numberOfMB = ParamUtil.getLong(actionRequest, "numberOfMB",0);
		categoryId = ParamUtil.getLong(actionRequest, "categoryId",MBCategoryConstants.DEFAULT_PARENT_CATEGORY_ID);
		subject = ParamUtil.getString(actionRequest, "subject","dummy subject");
		body = ParamUtil.getString(actionRequest, "body","dummy body");
		anonymous = ParamUtil.getBoolean(actionRequest, "anonymous",false);
		allowPingbacks = ParamUtil.getBoolean(actionRequest, "allowPingbacks",false);
		priority = ParamUtil.getDouble(actionRequest, "priority",0.0);
		parentCategoryId = ParamUtil.getLong(actionRequest, "parentCategoryId",MBCategoryConstants.DEFAULT_PARENT_CATEGORY_ID);
		categoryName = ParamUtil.getString(actionRequest, "categoryName","dummy Category Name");
		description = ParamUtil.getString(actionRequest, "description","dummy description");
		threadId = ParamUtil.getLong(actionRequest, "threadId",0);
		siteGroupId = ParamUtil.getLong(actionRequest, "siteGroupId",themeDisplay.getScopeGroupId());
		
		// Sites
		String[] groupsStrIds = ParamUtil.getStringValues(actionRequest, "groupIds",
				new String[] { String.valueOf(themeDisplay.getScopeGroupId()) });
		groupIds = CommonUtil.convertStringToLongArray(groupsStrIds);		
	}

	public long getNumberOfMB() {
		return numberOfMB;
	}

	public void setNumberOfMB(long numberOfMB) {
		this.numberOfMB = numberOfMB;
	}

	public long[] getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(long[] groupIds) {
		this.groupIds = groupIds;
	}

	public long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(long categoryId) {
		this.categoryId = categoryId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public boolean isAllowPingbacks() {
		return allowPingbacks;
	}

	public void setAllowPingbacks(boolean allowPingbacks) {
		this.allowPingbacks = allowPingbacks;
	}

	public double getPriority() {
		return priority;
	}

	public void setPriority(double priority) {
		this.priority = priority;
	}
	
	public long getParentCategoryId() {
		return parentCategoryId;
	}

	public void setParentCategoryId(long parentCategoryId) {
		this.parentCategoryId = parentCategoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public long getSiteGroupId() {
		return siteGroupId;
	}

	public void setSiteGroupId(long siteGroupId) {
		this.siteGroupId = siteGroupId;
	}

}
