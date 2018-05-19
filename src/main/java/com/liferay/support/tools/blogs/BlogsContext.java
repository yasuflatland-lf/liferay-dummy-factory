package com.liferay.support.tools.blogs;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class BlogsContext extends ParamContext {

	private long numberOfPosts = 0;
	private String baseTitle = "";
	private String contents = "";
	private long userId = 0;
	private long groupId = 0;

	public BlogsContext(ActionRequest actionRequest) {
		//Fetch data
		numberOfPosts = ParamUtil.getLong(actionRequest, "numberOfPosts",0);
		baseTitle = ParamUtil.getString(actionRequest, "baseTitle","");
		contents = ParamUtil.getString(actionRequest, "contents","");
		userId = ParamUtil.getLong(actionRequest, "userId",0);
		groupId = ParamUtil.getLong(actionRequest, "groupId",0);
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

}