package com.liferay.support.tools.user;

import com.github.javafaker.Faker;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.exception.UserScreenNameException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.access.control.AccessControlled;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.transaction.Isolation;
import com.liferay.portal.kernel.transaction.Transactional;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.utils.CommonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.annotation.ProviderType;

/**
 * User Data (User / Related roles applying) service
 * 
 * @author Yasuyuki Takeo
 */
@AccessControlled
@Component(service = UserDataService.class)
@ProviderType
@Transactional(
    isolation = Isolation.PORTAL, 
    rollbackFor = { 
        PortalException.class, 
        SystemException.class 
    }
)
public class UserDataService {

	/**
	 * Create User data
	 * 
	 * @param serviceContext
	 * @param organizationIds
	 * @param groupIds
	 * @param roleIds
	 * @param userGroupIds
	 * @param male
	 * @param fakerEnable
	 * @param password
	 * @param screenName
	 * @param emailAddress
	 * @param baseScreenName
	 * @param index
	 * @param localeStr
	 * @throws PortalException
	 */
	public void createUserData(ServiceContext serviceContext, long[] organizationIds, long[] groupIds, long[] roleIds,
			long[] userGroupIds, boolean male, boolean fakerEnable, String password, String screenName,
			String emailAddress, String baseScreenName, long index, String localeStr) throws PortalException {

		// For generating dummy user name
		Faker faker = _commonUtil.createFaker(localeStr);

		// Generate first / last name
		String firstName = (fakerEnable) ? faker.name().firstName() : baseScreenName;
		String lastName = (fakerEnable) ? faker.name().lastName() : String.valueOf(index);

		try {
			// Create User
			User user = _userLocalService.addUserWithWorkflow(
					serviceContext.getUserId(), 
					serviceContext.getCompanyId(), // companyId,
					false, // autoPassword,
					password, // password1,
					password, // password2,
					false, // autoScreenName,
					screenName.toString(), emailAddress.toString(), 0, // facebookId,
					StringPool.BLANK, // openId,
					LocaleUtil.getDefault(), // locale,
					firstName, // firstName,
					StringPool.BLANK, // middleName,
					lastName, // lastName,
					0, // prefixId,
					0, // suffixId,
					male, // male
					1, // birthdayMonth,
					1, // birthdayDay,
					1970, // birthdayYear,
					StringPool.BLANK, // jobTitle,
					groupIds,
					organizationIds, 
					getRegularRoleIds(roleIds), // this is only for reguler roles
					userGroupIds, // userGroupIds,
					false, // sendEmail
					serviceContext // serviceContext
			);

			if (_log.isDebugEnabled()) {
				_log.debug("user <" + user.getScreenName() + ">");
				_log.debug(user.toString());
				_log.debug("----------");
			}

			// Set site roles
			setSiteRoles(user.getUserId(), groupIds, roleIds);

			// Set org roles
			setOrgRoles(user.getUserId(), organizationIds, roleIds);

		} catch (UserScreenNameException e) {
			_log.error("User is duplicated. Skip : " + e.getMessage());
		}
	}


	/**
	 * Get Faker available locales
	 * 
	 * Filter Faker available locales based on Liferay available locales.
	 * @param locales
	 * @return Faker available locales based on Liferay available locales.
	 */
	public List<Locale> getFakerAvailableLocales(Set<Locale> locales) {
		List<String> fakerList = new ArrayList<>(
				Arrays.asList("bg", "ca", "ca-CAT", "da-DK", "de", "de-AT", "de-CH", "en", "en-AU", "en-au-ocker",
						"en-BORK", "en-CA", "en-GB", "en-IND", "en-NEP", "en-NG", "en-NZ", "en-PAK", "en-SG", "en-UG",
						"en-US", "en-ZA", "es", "es-MX", "fa", "fi-FI", "fr", "he", "in-ID", "it", "ja", "ko", "nb-NO",
						"nl", "pl", "pt", "pt-BR", "ru", "sk", "sv", "sv-SE", "tr", "uk", "vi", "zh-CN", "zh-TW"));
		return locales.stream()
				.filter(locale -> fakerList.contains(locale.getLanguage()))
				.collect(Collectors.toList());
	}

	/**
	 * Fetch regular roles
	 * 
	 * @param roleIds
	 * @return
	 * @throws PortalException
	 */
	public long[] getRegularRoleIds(long[] roleIds) throws PortalException {
		Map<Integer, List<Role>> roles = _commonUtil.filterRoles(roleIds);
		List<Role> regularRoles = roles.get(RoleConstants.TYPE_REGULAR);

		if (Validator.isNull(regularRoles) || regularRoles.size() == 0) {
			return null;
		}

		if (_log.isDebugEnabled()) {
			String regularids = regularRoles.stream().map(r -> String.valueOf(r.getRoleId()))
					.collect(Collectors.joining(","));
			_log.debug("Regular ids : " + regularids);
		}

		return regularRoles.stream().mapToLong(Role::getRoleId).toArray();
	}

	/**
	 * Set org roles
	 * 
	 * Filtering only organization roles out from role ids and asign
	 * organization roles to a user.
	 * 
	 * @param userId
	 * @param organizationIds
	 * @param roleIds
	 * @throws PortalException
	 */
	public void setOrgRoles(long userId, long[] organizationIds, long[] roleIds) throws PortalException {

		Map<Integer, List<Role>> roles = _commonUtil.filterRoles(roleIds);
		List<Role> orgRoles = roles.get(RoleConstants.TYPE_ORGANIZATION);

		if (Validator.isNull(orgRoles) || orgRoles.size() == 0) {
			return;
		}

		long[] orgIds = orgRoles.stream().mapToLong(Role::getRoleId).toArray();

		if (_log.isDebugEnabled()) {
			String orgids = orgRoles.stream().map(o -> String.valueOf(o.getPrimaryKey()))
					.collect(Collectors.joining(","));
			_log.debug("Organization ids : " + orgids);
		}

		if (0 == orgIds.length) {
			_log.debug("Organization didn't exist in the ids. exit");
			return;
		}

		List<Organization> orgs = _organizationLocalService.getOrganizations(organizationIds);
		long[] orgGroupdIds = orgs.stream().mapToLong(Organization::getGroupId).toArray();

		for (long orgGroupdId : orgGroupdIds) {
			_userGroupRoleLocalService.addUserGroupRoles(userId, orgGroupdId, orgIds);
		}
	}

	/**
	 * Set site roles
	 * 
	 * Filtering only site roles out from role ids and asign site roles to a
	 * user.
	 * 
	 * @param userId
	 * @param groupIds
	 * @param roleIds
	 * @throws PortalException
	 */
	public void setSiteRoles(long userId, long[] groupIds, long[] roleIds) throws PortalException {

		Map<Integer, List<Role>> roles = _commonUtil.filterRoles(roleIds);
		List<Role> siteRoles = roles.get(RoleConstants.TYPE_SITE);

		if (Validator.isNull(siteRoles) || siteRoles.size() == 0) {
			return;
		}

		long[] siteIds = siteRoles.stream().mapToLong(Role::getRoleId).toArray();

		if (_log.isDebugEnabled()) {
			String siteids = siteRoles.stream().map(s -> String.valueOf(s.getRoleId()))
					.collect(Collectors.joining(","));
			_log.debug("Site ids : " + siteids);
		}

		if (0 == siteIds.length) {
			_log.debug("Site roles didn't exist in the ids. exit");
			return;
		}

		for (long groupId : groupIds) {
			_userGroupRoleLocalService.addUserGroupRoles(userId, groupId, siteIds);
		}
	}

	@Reference
	private UserLocalService _userLocalService;
	
	@Reference
	private UserGroupRoleLocalService _userGroupRoleLocalService;
	
	@Reference
	private CommonUtil _commonUtil;
	
	@Reference
	private RoleLocalService _roleLocalService;
	
	@Reference
	private OrganizationLocalService _organizationLocalService;

	private static final Log _log = LogFactoryUtil.getLog(UserDataService.class);
}
