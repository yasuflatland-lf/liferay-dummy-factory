package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.DuplicateOrganizationException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Organizations
 * 
 * @author Yasuyuki Takeo
 */
@Component(
	immediate = true, 
	property = { 
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.ORGANIZAION
	}, 
	service = MVCActionCommand.class
)
public class OrganizationMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Create Organizations
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException 
	 */
	private void createOrganizations(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException{
		//Parameber values
		long startIndex = 1;
		long numberOfOrganizations = 0;
		String baseOrganizationName = "";
		int parentOrganizationId = OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID;
		
		//Fetch data
		startIndex = ParamUtil.getLong(actionRequest, "startIndex",1);
		numberOfOrganizations = ParamUtil.getLong(actionRequest, "numberOfOrganizations",0);
		baseOrganizationName = ParamUtil.getString(actionRequest, "baseOrganizationName","");
		parentOrganizationId = ParamUtil.getInteger(actionRequest, "parentOrganizationId", OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID);
		
		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Organization.class.getName(), actionRequest);				

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(actionRequest, 0);

		System.out.println("Starting to create " + numberOfOrganizations + " organizations");

		for (long i = startIndex; i <= numberOfOrganizations; i++) {
			//Update progress
			progressManager.trackProgress(i, numberOfOrganizations);

			//Create Organization Name
			StringBundler organizationName = new StringBundler(2);
			organizationName.append(i).append(baseOrganizationName);

			try {
				
				_organizationLocalService.addOrganization(
						serviceContext.getUserId(),
						parentOrganizationId, // parentOrganizationId
						organizationName.toString(), // name
						false); // site
				
			} catch (Exception e) {
				if (e instanceof DuplicateOrganizationException ) {
					_log.error("Organizations <" + organizationName.toString() + "> is duplicated. Skip : " + e.getMessage());
				}
				else {
					//Finish progress
					progressManager.finish();	
					throw e;
				}
			} 
		}
		
		//Finish progress
		progressManager.finish();	

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfOrganizations + " organizations");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		try {
			//Create Organization
			createOrganizations(actionRequest, actionResponse);
			
		} catch (Exception e) {
			hideDefaultSuccessMessage(actionRequest);
			_log.error(e,e);
		}
	
	}
	
	@Reference
	private Portal _portal;
	
	@Reference
	private OrganizationLocalService _organizationLocalService;	
	
	private static final Log _log = LogFactoryUtil.getLog(
			OrganizationMVCActionCommand.class);		
}
