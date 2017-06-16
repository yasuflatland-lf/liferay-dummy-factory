package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.constants.LDFPortletKeys;

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
	private void createOrganizations(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException {

		double loader = 10;

		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Organization.class.getName(), actionRequest);				

		System.out.println("Starting to create " + numberOfOrganizations + " organizations");

		for (long i = 1; i <= numberOfOrganizations; i++) {
			if (numberOfOrganizations >= 100) {
				if (i == (int) (numberOfOrganizations * (loader / 100))) {
					System.out.println("Creating organizations..." + (int) loader + "% done");
					loader = loader + 10;
				}
			}

			//Create Organization Name
			StringBundler organizationName = new StringBundler(2);
			organizationName.append(baseOrganizationName).append(i);

			_organizationLocalService.addOrganization(
					serviceContext.getUserId(),
					parentOrganizationId, // parentOrganizationId
					organizationName.toString(), // name
					false); // site

		}

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfOrganizations + " organizations");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		try {
			//Fetch data
			numberOfOrganizations = ParamUtil.getLong(actionRequest, "numberOfOrganizations");
			baseOrganizationName = ParamUtil.getString(actionRequest, "baseOrganizationName");
			parentOrganizationId = ParamUtil.getInteger(actionRequest, "parentOrganizationId", OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID);

			//Create Organization
			createOrganizations(actionRequest, actionResponse);
		} catch (Throwable e) {
			hideDefaultSuccessMessage(actionRequest);
			e.printStackTrace();
		}
	
	}
	
	@Reference(unbind = "-")
	protected void setOrganizationService(
			OrganizationLocalService organizationLocalService) {
		_organizationLocalService = organizationLocalService;
	}

	private OrganizationLocalService _organizationLocalService;	

	private long numberOfOrganizations = 0;
	private String baseOrganizationName = "";
	private int parentOrganizationId = OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID;
	
}
