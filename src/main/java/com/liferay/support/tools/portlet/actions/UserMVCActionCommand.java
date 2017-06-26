package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.UserScreenNameException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.CommonUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Users
 * 
 * @author Yasuyuki Takeo
 */
@Component(
    immediate = true, 
    property = { 
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
        "mvc.command.name=" + LDFPortletKeys.USERS
    }, 
    service = MVCActionCommand.class
)
public class UserMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Create Users
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException
	 */
	private void createUsers(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException {
		double loader = 10;

		ServiceContext serviceContext = ServiceContextFactory.getInstance(Group.class.getName(), actionRequest);

		System.out.println("Starting to create " + numberOfusers + " users");

		for (long i = 1; i <= numberOfusers; i++) {
			if (numberOfusers >= 100) {
				if (i == (int) (numberOfusers * (loader / 100))) {
					System.out.println("Creating users..." + (int) loader + "% done");
					loader = loader + 10;
				}
			}

			StringBundler screenName = new StringBundler(2);
			screenName.append(baseScreenName);
			screenName.append(i);

			StringBundler emailAddress = new StringBundler(2);
			emailAddress.append(screenName);
			emailAddress.append("@liferay.com");

			try {
				// Create User
				User user = _userLocalService.addUserWithWorkflow(serviceContext.getUserId(),
						serviceContext.getCompanyId(), // companyId,
						false, // autoPassword,
						password, // password1,
						password, // password2,
						false, // autoScreenName,
						screenName.toString(), 
						emailAddress.toString(), 
						0, // facebookId,
						StringPool.BLANK, // openId,
						LocaleUtil.getDefault(), // locale,
						baseScreenName, // firstName,
						StringPool.BLANK, // middleName,
						String.valueOf(i), // lastName,
						0, // prefixId,
						0, // suffixId,
						male, // male
						1, // birthdayMonth,
						1, // birthdayDay,
						1970, // birthdayYear,
						StringPool.BLANK, // jobTitle,
						groupIds, 
						organizationIds, 
						getRegularRoleIds(roleIds), // roleIds, this is only for regular roles
						userGroupIds, // userGroupIds,
						false, // sendEmail
						serviceContext // serviceContext
				);

				if(_log.isDebugEnabled()) {
					_log.debug("user <" + user.getScreenName() + ">");
					_log.debug(user.toString());
					_log.debug("----------");
				}
				
				// Set site roles
				setSiteRoles(user.getUserId(),groupIds,roleIds);

				// Set org roles
				setOrgRoles(user.getUserId(), organizationIds, roleIds);
				
			} catch (UserScreenNameException e) {
				_log.error("User is duplicated. Skip : " + e.getMessage());
			}

		}

		SessionMessages.add(actionRequest, "success");

		System.out.println("Finished creating " + numberOfusers + " users");
	}
	
	/**
	 * Fetch regular roles
	 * 
	 * @param roleIds
	 * @return
	 * @throws PortalException
	 */
	protected long[] getRegularRoleIds(long[] roleIds) throws PortalException {
		Map<Integer, List<Role>> roles = _commonUtil.filterRoles(roleIds);
        List<Role> regularRoles = roles.get(RoleConstants.TYPE_REGULAR);
        
        if(Validator.isNull(regularRoles) || regularRoles.size() == 0 ) {
        	return null;
        }
        
        if(_log.isDebugEnabled()) {
        	String regularids = regularRoles.stream()
		      .map(r -> String.valueOf(r.getRoleId()))
		      .collect(Collectors.joining(","));        	
        	_log.debug("Regular ids : " + regularids);
        }
        
        return regularRoles.stream().mapToLong(Role::getRoleId).toArray();		
	}
	
	/**
	 * Set org roles
	 * 
	 * Filtering only organization roles out from role ids and asign organization roles to a user.
	 * 
	 * @param userId
	 * @param organizationIds
	 * @param roleIds
	 * @throws PortalException
	 */
	protected void setOrgRoles(long userId, long[] organizationIds, long[] roleIds) throws PortalException {

		Map<Integer, List<Role>> roles = _commonUtil.filterRoles(roleIds);
        List<Role> orgRoles = roles.get(RoleConstants.TYPE_ORGANIZATION);

        if(Validator.isNull(orgRoles) || orgRoles.size() == 0 ) {
        	return;
        }
        
        long[] orgIds = orgRoles.stream().mapToLong(Role::getRoleId).toArray();
        
        if(_log.isDebugEnabled()) {
        	String orgids = orgRoles.stream()
		      .map(o -> String.valueOf(o.getPrimaryKey()))
		      .collect(Collectors.joining(","));        	
        	_log.debug("Organization ids : " + orgids);
        }
        
        if(0 == orgIds.length) {
        	_log.debug("Organization didn't exist in the ids. exit");
        	return;
        }
        
        List<Organization> orgs = _organizationLocalService.getOrganizations(organizationIds);
        long[] orgGroupdIds = orgs.stream().mapToLong(Organization::getGroupId).toArray();
        
		for(long orgGroupdId : orgGroupdIds ) {
			_userGroupRoleLocalService.addUserGroupRoles(userId, orgGroupdId, orgIds);
		}        
	}	
	
	/**
	 * Set site roles
	 * 
	 * Filtering only site roles out from role ids and asign site roles to a user.
	 * 
	 * @param userId
	 * @param groupIds
	 * @param roleIds
	 * @throws PortalException
	 */
	protected void setSiteRoles(long userId, long[] groupIds, long[] roleIds) throws PortalException {

		Map<Integer, List<Role>> roles = _commonUtil.filterRoles(roleIds);
		List<Role> siteRoles = roles.get(RoleConstants.TYPE_SITE);
		
        if(Validator.isNull(siteRoles) || siteRoles.size() == 0 ) {
        	return;
        }
        
        long[] siteIds = siteRoles.stream().mapToLong(Role::getRoleId).toArray();
    
        if(_log.isDebugEnabled()) {
        	String siteids = siteRoles.stream()
		      .map(s -> String.valueOf(s.getRoleId()))
		      .collect(Collectors.joining(","));        	
        	_log.debug("Site ids : " + siteids);
        }
        
        if(0 == siteIds.length) {
        	_log.debug("Site roles didn't exist in the ids. exit");
        	return;
        }
        
		for(long groupId : groupIds ) {
			_userGroupRoleLocalService.addUserGroupRoles(userId, groupId, siteIds);
		}        
	}		
	
	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {
		try {
			// Fetch data
			numberOfusers = ParamUtil.getLong(actionRequest, "numberOfusers", 0);
			baseScreenName = ParamUtil.getString(actionRequest, "baseScreenName", "");
			male = ParamUtil.getBoolean(actionRequest, "male", true);
			password = ParamUtil.getString(actionRequest, "password", "test");

			// Organization
			String[] organizations = ParamUtil.getStringValues(actionRequest, "organizations", null);
			organizationIds = _commonUtil.convertStringToLongArray(organizations);

			// Sites
			String[] groups = ParamUtil.getStringValues(actionRequest, "groups", null);
			groupIds = _commonUtil.convertStringToLongArray(groups);

			// Roles
			String[] roles = ParamUtil.getStringValues(actionRequest, "roles", null);
			roleIds = _commonUtil.convertStringToLongArray(roles);

			// User Group
			String[] userGroups = ParamUtil.getStringValues(actionRequest, "userGroups", null);
			userGroupIds = _commonUtil.convertStringToLongArray(userGroups);

			// Create users
			createUsers(actionRequest, actionResponse);
		} catch (Exception e) {
			hideDefaultSuccessMessage(actionRequest);
			_log.error(e,e);
		}

		actionResponse.setRenderParameter("mvcRenderCommandName", LDFPortletKeys.COMMON);
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
	
	private long numberOfusers = 0;
	private String baseScreenName = "";
	private long[] organizationIds = null;
	private long[] groupIds = null;
	private long[] roleIds = null;
	private long[] userGroupIds = null;
	private boolean male;
	private String password;

	private static final Log _log = LogFactoryUtil.getLog(UserMVCActionCommand.class);
}
