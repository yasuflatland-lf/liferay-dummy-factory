package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.CommonUtil;

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

		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Group.class.getName(), actionRequest);				

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
			
			//Create User
			_userService.addUserWithWorkflow(
					serviceContext.getCompanyId(), //companyId,
					false, //autoPassword,
					password, //password1, 
					password, //password2, 
					false, //autoScreenName, 
					screenName.toString(),
					emailAddress.toString(),
					0, //facebookId, 
					StringPool.BLANK, //openId, 
					LocaleUtil.getDefault(), //locale, 
					baseScreenName, //firstName, 
					StringPool.BLANK, //middleName, 
					String.valueOf(i), //lastName, 
					0, //prefixId, 
					0, //suffixId, 
					male, // male
					1, //birthdayMonth, 
					1, //birthdayDay, 
					1970, //birthdayYear, 
					StringPool.BLANK,//jobTitle, 
					groupIds, 
					organizationIds, 
					roleIds, 
					userGroupIds, //userGroupIds, 
					false, //sendEmail
					serviceContext // serviceContext
					);
			
		}

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfusers + " users");
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
		} catch (Throwable e) {
			hideDefaultSuccessMessage(actionRequest);
			e.printStackTrace();
		}

	}

	@Reference(unbind = "-")
	protected void setUserService(UserService userService) {
		_userService = userService;
	}

	@Reference(unbind = "-")
	public void setCommonUtil(CommonUtil commonUtil) {
		_commonUtil = commonUtil;
	}

	private UserService _userService;
	private CommonUtil _commonUtil;	

	private long numberOfusers = 0;
	private String baseScreenName = "";
	private long[] organizationIds = null;
	private long[] groupIds = null;
	private long[] roleIds = null;
	private long[] userGroupIds = null;
	private boolean male;
	private String password;
}
