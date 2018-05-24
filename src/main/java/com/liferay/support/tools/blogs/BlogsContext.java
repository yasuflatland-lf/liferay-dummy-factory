package com.liferay.support.tools.blogs;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;
import com.liferay.support.tools.utils.CommonUtil;

import javax.portlet.ActionRequest;

public class BlogsContext extends ParamContext {

	private long numberOfPosts = 0;
	private String baseTitle = "";
	private String contents = "";
	private long userId = 0;
	private long groupId = 0;
	private String[] allowTrackbacks;
	private boolean allowPingbacks;

	public BlogsContext(ActionRequest actionRequest) {
		//Fetch data
		numberOfPosts = ParamUtil.getLong(actionRequest, "numberOfPosts",0);
		baseTitle = ParamUtil.getString(actionRequest, "baseTitle","");
		contents = ParamUtil.getString(actionRequest, "contents","");
		userId = ParamUtil.getLong(actionRequest, "userId",0);
		groupId = ParamUtil.getLong(actionRequest, "groupId",0);
		String tempPings = ParamUtil.getString(actionRequest, "allowTrackbacks","");
		allowTrackbacks = CommonUtil.convertToStringArray(tempPings);
		allowPingbacks = ParamUtil.getBoolean(actionRequest, "allowPingbacks", true);
	}

	public long getNumberOfPosts() {
		return numberOfPosts;
	}

	public void setNumberOfPosts(long numberOfPosts) {
		this.numberOfPosts = numberOfPosts;
	}

	public String getBaseTitle() {
		return baseTitle;
	}

	public void setBaseTitle(String baseTitle) {
		this.baseTitle = baseTitle;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public String[] getAllowTrackbacks() {
		return allowTrackbacks;
	}

	public void setAllowTrackbacks(String[] allowTrackbacks) {
		this.allowTrackbacks = allowTrackbacks;
	}

	public boolean isAllowTrackbacks() {
		return (0 == this.allowTrackbacks.length) ? false : true;
	}

	public boolean isAllowPingbacks() {
		return allowPingbacks;
	}

	public void setAllowPingbacks(boolean allowPingbacks) {
		this.allowPingbacks = allowPingbacks;
	}
}