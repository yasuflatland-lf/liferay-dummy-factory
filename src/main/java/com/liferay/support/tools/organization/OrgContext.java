package com.liferay.support.tools.organization;

import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class OrgContext extends ParamContext {
	
	private long startIndex = 1;
	private long numberOfOrganizations = 0;
	private String baseOrganizationName = "";
	private int parentOrganizationId = OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID;
	private boolean organizationSiteCreate = false;
	
	public OrgContext(ActionRequest actionRequest) {
		//Fetch data
		startIndex = ParamUtil.getLong(actionRequest, "startIndex",1);
		numberOfOrganizations = ParamUtil.getLong(actionRequest, "numberOfOrganizations",0);
		baseOrganizationName = ParamUtil.getString(actionRequest, "baseOrganizationName","");
		parentOrganizationId = ParamUtil.getInteger(actionRequest, "parentOrganizationId", OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID);
		organizationSiteCreate = ParamUtil.getBoolean(actionRequest, "organizationSiteCreate", false);
	}

	public long getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(long startIndex) {
		this.startIndex = startIndex;
	}

	public long getNumberOfOrganizations() {
		return numberOfOrganizations;
	}

	public void setNumberOfOrganizations(long numberOfOrganizations) {
		this.numberOfOrganizations = numberOfOrganizations;
	}

	public String getBaseOrganizationName() {
		return baseOrganizationName;
	}

	public void setBaseOrganizationName(String baseOrganizationName) {
		this.baseOrganizationName = baseOrganizationName;
	}

	public int getParentOrganizationId() {
		return parentOrganizationId;
	}

	public void setParentOrganizationId(int parentOrganizationId) {
		this.parentOrganizationId = parentOrganizationId;
	}

	public boolean isOrganizationSiteCreate() {
		return organizationSiteCreate;
	}

	public void setOrganizationSiteCreate(boolean organizationSiteCreate) {
		this.organizationSiteCreate = organizationSiteCreate;
	}
}
