
package com.liferay.support.tools.user;

import com.liferay.announcements.kernel.model.AnnouncementsDelivery;
import com.liferay.announcements.kernel.model.AnnouncementsEntryConstants;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portlet.announcements.model.impl.AnnouncementsDeliveryImpl;
import com.liferay.support.tools.common.ParamContext;
import com.liferay.support.tools.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;

public class UserContext extends ParamContext {

	public UserContext(ActionRequest actionRequest)
		throws PortalException {

		// Fetch data
		numberOfusers = ParamUtil.getLong(actionRequest, "numberOfusers", 0);
		baseScreenName =
			ParamUtil.getString(actionRequest, "baseScreenName", "");
		baseDomain =
			ParamUtil.getString(actionRequest, "baseDomain", "liferay.com");
		male = ParamUtil.getBoolean(actionRequest, "male", true);
		fakerEnable = ParamUtil.getBoolean(actionRequest, "fakerEnable", false);
		password = ParamUtil.getString(actionRequest, "password", "test");
		locale = ParamUtil.getString(actionRequest, "locale", "en");
		autoUserPreLogin =
			ParamUtil.getBoolean(actionRequest, "autoUserPreLogin", false);

		// Organization
		String[] organizations =
			ParamUtil.getStringValues(actionRequest, "organizations", null);
		organizationIds = CommonUtil.convertStringToLongArray(organizations);

		// Sites
		String[] groups =
			ParamUtil.getStringValues(actionRequest, "groups", null);
		groupIds = CommonUtil.convertStringToLongArray(groups);

		// Roles
		String[] roles =
			ParamUtil.getStringValues(actionRequest, "roles", null);
		roleIds = CommonUtil.convertStringToLongArray(roles);

		// User Group
		String[] userGroups =
			ParamUtil.getStringValues(actionRequest, "userGroups", null);
		userGroupIds = CommonUtil.convertStringToLongArray(userGroups);

		// Announcements Deliveries
		announcementsDeliveries = getAnnouncementsDeliveries(actionRequest);

		// Site Templates for My Profile and My Dashboard
		publicLayoutSetPrototypeId =
			ParamUtil.getLong(actionRequest, "publicLayoutSetPrototypeId");

		setPublicLayoutSetPrototypeLinkEnabled(
			(0 == publicLayoutSetPrototypeId) ? false : true);

		privateLayoutSetPrototypeId =
			ParamUtil.getLong(actionRequest, "privateLayoutSetPrototypeId");

		setPrivateLayoutSetPrototypeLinkEnabled(
			(0 == privateLayoutSetPrototypeId) ? false : true);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			Group.class.getName(), actionRequest);
		setServiceContext(serviceContext);
	}

	/**
	 * Get Announcements Deliveries
	 * 
	 * @param actionRequest
	 * @return List of AnnouncementsDelivery
	 */
	protected List<AnnouncementsDelivery> getAnnouncementsDeliveries(
		ActionRequest actionRequest) {

		List<AnnouncementsDelivery> announcementsDeliveries = new ArrayList<>();

		for (String type : AnnouncementsEntryConstants.TYPES) {
			boolean email = ParamUtil.getBoolean(
				actionRequest, "announcementsType" + type + "Email");
			boolean sms = ParamUtil.getBoolean(
				actionRequest, "announcementsType" + type + "Sms");

			AnnouncementsDelivery announcementsDelivery =
				new AnnouncementsDeliveryImpl();

			announcementsDelivery.setType(type);
			announcementsDelivery.setEmail(email);
			announcementsDelivery.setSms(sms);

			announcementsDeliveries.add(announcementsDelivery);
		}

		return announcementsDeliveries;
	}

	public long getNumberOfusers() {

		return numberOfusers;
	}

	public void setNumberOfusers(long numberOfusers) {

		this.numberOfusers = numberOfusers;
	}

	public String getBaseScreenName() {

		return baseScreenName;
	}

	public void setBaseScreenName(String baseScreenName) {

		this.baseScreenName = baseScreenName;
	}

	public String getBaseDomain() {

		return baseDomain;
	}

	public void setBaseDomain(String baseDomain) {

		this.baseDomain = baseDomain;
	}

	public long[] getOrganizationIds() {

		return organizationIds;
	}

	public void setOrganizationIds(long[] organizationIds) {

		this.organizationIds = organizationIds;
	}

	public long[] getGroupIds() {

		return groupIds;
	}

	public void setGroupIds(long[] groupIds) {

		this.groupIds = groupIds;
	}

	public long[] getRoleIds() {

		return roleIds;
	}

	public void setRoleIds(long[] roleIds) {

		this.roleIds = roleIds;
	}

	public long[] getUserGroupIds() {

		return userGroupIds;
	}

	public void setUserGroupIds(long[] userGroupIds) {

		this.userGroupIds = userGroupIds;
	}

	public boolean isMale() {

		return male;
	}

	public void setMale(boolean male) {

		this.male = male;
	}

	public boolean isFakerEnable() {

		return fakerEnable;
	}

	public void setFakerEnable(boolean fakerEnable) {

		this.fakerEnable = fakerEnable;
	}

	public String getPassword() {

		return password;
	}

	public void setPassword(String password) {

		this.password = password;
	}

	public String getLocale() {

		return locale;
	}

	public void setLocale(String locale) {

		this.locale = locale;
	}

	public boolean isAutoUserPreLogin() {

		return autoUserPreLogin;
	}

	public void setAutoUserPreLogin(boolean autoUserPreLogin) {

		this.autoUserPreLogin = autoUserPreLogin;
	}

	public List<AnnouncementsDelivery> getAnnouncementsDeliveries() {

		return announcementsDeliveries;
	}

	public void setAnnouncementsDeliveries(
		List<AnnouncementsDelivery> announcementsDeliveries) {

		this.announcementsDeliveries = announcementsDeliveries;
	}

	public long getPublicLayoutSetPrototypeId() {

		return publicLayoutSetPrototypeId;
	}

	public void setPublicLayoutSetPrototypeId(long publicLayoutSetPrototypeId) {

		this.publicLayoutSetPrototypeId = publicLayoutSetPrototypeId;
	}

	public long getPrivateLayoutSetPrototypeId() {

		return privateLayoutSetPrototypeId;
	}

	public void setPrivateLayoutSetPrototypeId(
		long privateLayoutSetPrototypeId) {

		this.privateLayoutSetPrototypeId = privateLayoutSetPrototypeId;
	}

	public boolean isPublicLayoutSetPrototypeLinkEnabled() {

		return publicLayoutSetPrototypeLinkEnabled;
	}

	public void setPublicLayoutSetPrototypeLinkEnabled(
		boolean publicLayoutSetPrototypeLinkEnabled) {

		this.publicLayoutSetPrototypeLinkEnabled =
			publicLayoutSetPrototypeLinkEnabled;
	}

	public boolean isPrivateLayoutSetPrototypeLinkEnabled() {

		return privateLayoutSetPrototypeLinkEnabled;
	}

	public void setPrivateLayoutSetPrototypeLinkEnabled(
		boolean privateLayoutSetPrototypeLinkEnabled) {

		this.privateLayoutSetPrototypeLinkEnabled =
			privateLayoutSetPrototypeLinkEnabled;
	}

	private long numberOfusers = 0;
	private String baseScreenName = "";
	private String baseDomain = "";
	private long[] organizationIds = null;
	private long[] groupIds = null;
	private long[] roleIds = null;
	private long[] userGroupIds = null;
	private boolean male;
	private boolean fakerEnable;
	private String password;
	private String locale;
	private boolean autoUserPreLogin;
	private List<AnnouncementsDelivery> announcementsDeliveries =
		new ArrayList<>();
	private long publicLayoutSetPrototypeId;
	private long privateLayoutSetPrototypeId;
	private boolean publicLayoutSetPrototypeLinkEnabled;
	private boolean privateLayoutSetPrototypeLinkEnabled;

}
