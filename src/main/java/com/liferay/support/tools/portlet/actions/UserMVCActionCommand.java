package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.GroupFriendlyURLException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.user.UserDataService;
import com.liferay.support.tools.utils.CommonUtil;
import com.liferay.support.tools.utils.ProgressManager;

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
	private void createUsers(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		long numberOfusers = 0;
		String baseScreenName = "";
		String baseDomain = "";
		long[] organizationIds = null;
		long[] groupIds = null;
		long[] roleIds = null;
		long[] userGroupIds = null;
		boolean male;
		boolean fakerEnable;
		String password;
		String locale;

		// Fetch data
		numberOfusers = ParamUtil.getLong(actionRequest, "numberOfusers", 0);
		baseScreenName = ParamUtil.getString(actionRequest, "baseScreenName", "");
		baseDomain = ParamUtil.getString(actionRequest, "baseDomain","liferay.com");
		male = ParamUtil.getBoolean(actionRequest, "male", true);
		fakerEnable = ParamUtil.getBoolean(actionRequest, "fakerEnable", false);
		password = ParamUtil.getString(actionRequest, "password", "test");
		locale = ParamUtil.getString(actionRequest, "locale", "en");

		// Organization
		String[] organizations = ParamUtil.getStringValues(actionRequest, "organizations", null);
		organizationIds = CommonUtil.convertStringToLongArray(organizations);

		// Sites
		String[] groups = ParamUtil.getStringValues(actionRequest, "groups", null);
		groupIds = CommonUtil.convertStringToLongArray(groups);

		// Roles
		String[] roles = ParamUtil.getStringValues(actionRequest, "roles", null);
		roleIds = CommonUtil.convertStringToLongArray(roles);

		// User Group
		String[] userGroups = ParamUtil.getStringValues(actionRequest, "userGroups", null);
		userGroupIds = CommonUtil.convertStringToLongArray(userGroups);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(Group.class.getName(), actionRequest);

		// Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(actionRequest);

		System.out.println("Starting to create " + numberOfusers + " users");

		for (long i = 1; i <= numberOfusers; i++) {
			// Update progress
			progressManager.trackProgress(i, numberOfusers);

			StringBundler screenName = new StringBundler(2);
			screenName.append(baseScreenName);

			// Add number more then one user
			if (1 < numberOfusers) {
				screenName.append(i);
			}

			StringBundler emailAddress = new StringBundler(2);
			emailAddress.append(screenName);
			emailAddress.append("@").append(baseDomain);

			try {
				// Create user and apply roles
				_userDataService.createUserData(
						serviceContext, 
						organizationIds, 
						groupIds, 
						roleIds, 
						userGroupIds, 
						male,
						fakerEnable, 
						password, 
						screenName.toString(), 
						emailAddress.toString(), 
						baseScreenName, 
						i,
						locale);
				
			} catch (Exception e) {
				
				// Finish progress
				progressManager.finish();
				
				if (e instanceof GroupFriendlyURLException) {
					SessionErrors.add(actionRequest, "group-friendly-url-error");
					hideDefaultSuccessMessage(actionRequest);
				}
				
				e.printStackTrace();
				return;
			}
		}

		// Finish progress
		progressManager.finish();

		SessionMessages.add(actionRequest, "success");

		System.out.println("Finished creating " + numberOfusers + " users");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {
		try {
			// Create users
			createUsers(actionRequest, actionResponse);

		} catch (Exception e) {
			hideDefaultSuccessMessage(actionRequest);
			_log.error(e, e);
		}

		actionResponse.setRenderParameter("mvcRenderCommandName", LDFPortletKeys.COMMON);
	}

	@Reference
	private UserDataService _userDataService;

	private static final Log _log = LogFactoryUtil.getLog(UserMVCActionCommand.class);
}
