package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;

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
			
		}

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfusers + " users");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		try {
			//Fetch data
			numberOfusers = ParamUtil.getLong(actionRequest, "numberOfusers",1);
			baseUserName = ParamUtil.getString(actionRequest, "baseUserName","");

			//Create users
			createUsers(actionRequest, actionResponse);
		} catch (Throwable e) {
			hideDefaultSuccessMessage(actionRequest);
			e.printStackTrace();
		}
	
	}
	
	@Reference(unbind = "-")
	protected void setUserLocalService(
			UserLocalService userLocalService) {
		_userLocalService = userLocalService;
	}

	private UserLocalService _userLocalService;	

	private long numberOfusers = 0;
	private String baseUserName = "";
}
