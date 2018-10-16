
package com.liferay.support.tools.site;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class SiteContext extends ParamContext {

	private long numberOfSites = 0;
	private String baseSiteName = StringPool.BLANK;
	private int siteType = GroupConstants.TYPE_SITE_OPEN;
	private long parentGroupId = GroupConstants.DEFAULT_PARENT_GROUP_ID;
	private long liveGroupId = GroupConstants.DEFAULT_LIVE_GROUP_ID;
	private boolean manualMembership = false;
	private boolean site = true;
	private boolean inheritContent = false;
	private boolean active = true;
	private long publicLayoutSetPrototypeId;
	private boolean publicLayoutSetPrototypeLinkEnabled;

	public SiteContext(ActionRequest actionRequest) {

		// Fetch data
		numberOfSites = ParamUtil.getLong(actionRequest, "numberOfSites", 0);
		baseSiteName =
			ParamUtil.getString(actionRequest, "baseSiteName", "dummy");
		siteType = ParamUtil.getInteger(
			actionRequest, "siteType", GroupConstants.TYPE_SITE_OPEN);
		parentGroupId = ParamUtil.getLong(
			actionRequest, "parentGroupId",
			GroupConstants.DEFAULT_PARENT_GROUP_ID);
		liveGroupId = ParamUtil.getLong(
			actionRequest, "liveGroupId", GroupConstants.DEFAULT_LIVE_GROUP_ID);

		manualMembership =
			ParamUtil.getBoolean(actionRequest, "manualMembership", true);
		site = ParamUtil.getBoolean(actionRequest, "site", true);
		inheritContent =
			ParamUtil.getBoolean(actionRequest, "inheritContent", false);
		active = ParamUtil.getBoolean(actionRequest, "active", true);
		
		// Site Templates 
		publicLayoutSetPrototypeId =
			ParamUtil.getLong(actionRequest, "publicLayoutSetPrototypeId");

		setPublicLayoutSetPrototypeLinkEnabled(
			(0 == publicLayoutSetPrototypeId) ? false : true);		
	}

	public long getPublicLayoutSetPrototypeId() {

		return publicLayoutSetPrototypeId;
	}

	public void setPublicLayoutSetPrototypeId(long publicLayoutSetPrototypeId) {

		this.publicLayoutSetPrototypeId = publicLayoutSetPrototypeId;
	}

	public boolean isPublicLayoutSetPrototypeLinkEnabled() {

		return publicLayoutSetPrototypeLinkEnabled;
	}

	public void setPublicLayoutSetPrototypeLinkEnabled(
		boolean publicLayoutSetPrototypeLinkEnabled) {

		this.publicLayoutSetPrototypeLinkEnabled =
			publicLayoutSetPrototypeLinkEnabled;
	}

	public long getNumberOfSites() {

		return numberOfSites;
	}

	public void setNumberOfSites(long numberOfSites) {

		this.numberOfSites = numberOfSites;
	}

	public String getBaseSiteName() {

		return baseSiteName;
	}

	public void setBaseSiteName(String baseSiteName) {

		this.baseSiteName = baseSiteName;
	}

	public int getSiteType() {

		return siteType;
	}

	public void setSiteType(int siteType) {

		this.siteType = siteType;
	}

	public long getParentGroupId() {

		return parentGroupId;
	}

	public void setParentGroupId(long parentGroupId) {

		this.parentGroupId = parentGroupId;
	}

	public long getLiveGroupId() {

		return liveGroupId;
	}

	public void setLiveGroupId(long liveGroupId) {

		this.liveGroupId = liveGroupId;
	}

	public boolean isManualMembership() {

		return manualMembership;
	}

	public void setManualMembership(boolean manualMembership) {

		this.manualMembership = manualMembership;
	}

	public boolean isSite() {

		return site;
	}

	public void setSite(boolean site) {

		this.site = site;
	}

	public boolean isInheritContent() {

		return inheritContent;
	}

	public void setInheritContent(boolean inheritContent) {

		this.inheritContent = inheritContent;
	}

	public boolean isActive() {

		return active;
	}

	public void setActive(boolean active) {

		this.active = active;
	}

}
