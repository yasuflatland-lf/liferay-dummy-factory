package com.liferay.support.tools.wiki;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class WikiContext extends ParamContext {

	private long groupId = 0;
	private long createContentsType;
	
	private long numberOfnodes = 0;
	private String baseNodeName = "";

	private long numberOfpages;
	private String basePageName;
	private String baseContentName;
	private String baseSummaryName;
	private boolean minorEdit;
	private long nodeId;
	private long resourcePrimKey;
	private String format;

	public WikiContext(ActionRequest actionRequest) {
		// Common
		groupId = ParamUtil.getLong(actionRequest, "groupId",0);
		createContentsType = ParamUtil.getLong(actionRequest, "createContentsType",0);

		//Node
		numberOfnodes = ParamUtil.getLong(actionRequest, "numberOfnodes",0);
		baseNodeName = ParamUtil.getString(actionRequest, "baseNodeName","");
		
		//Page
		numberOfpages = ParamUtil.getLong(actionRequest, "numberOfpages",0);
		nodeId = ParamUtil.getLong(actionRequest, "nodeId",0);
		resourcePrimKey = ParamUtil.getLong(actionRequest, "resourcePrimKey",0);
		format = ParamUtil.getString(actionRequest, "format","");
		basePageName = ParamUtil.getString(actionRequest, "basePageName","");
		baseContentName = ParamUtil.getString(actionRequest, "baseContentName","");
		baseSummaryName = ParamUtil.getString(actionRequest, "baseSummaryName","");		
		minorEdit = ParamUtil.getBoolean(actionRequest, "minorEdit", false);
	}
	
	public long getNumberOfnodes() {
		return numberOfnodes;
	}

	public void setNumberOfnodes(long numberOfnodes) {
		this.numberOfnodes = numberOfnodes;
	}

	public String getBaseNodeName() {
		return baseNodeName;
	}

	public void setBaseNodeName(String baseNodeName) {
		this.baseNodeName = baseNodeName;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
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

	public String getBaseContentName() {
		return baseContentName;
	}

	public void setBaseContentName(String baseContentName) {
		this.baseContentName = baseContentName;
	}

	public String getBaseSummaryName() {
		return baseSummaryName;
	}

	public void setBaseSummaryName(String baseSummaryName) {
		this.baseSummaryName = baseSummaryName;
	}

	public boolean isMinorEdit() {
		return minorEdit;
	}

	public void setMinorEdit(boolean minorEdit) {
		this.minorEdit = minorEdit;
	}

	public long getCreateContentsType() {
		return createContentsType;
	}

	public void setCreateContentsType(long createContentsType) {
		this.createContentsType = createContentsType;
	}

	public long getNodeId() {
		return nodeId;
	}

	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	public long getResourcePrimKey() {
		return resourcePrimKey;
	}

	public void setResourcePrimKey(long resourcePrimKey) {
		this.resourcePrimKey = resourcePrimKey;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}	
}
