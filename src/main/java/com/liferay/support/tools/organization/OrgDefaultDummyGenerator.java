package com.liferay.support.tools.organization;

import com.liferay.portal.kernel.exception.DuplicateOrganizationException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Organization Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = OrgDefaultDummyGenerator.class)
public class OrgDefaultDummyGenerator extends DummyGenerator<OrgContext> {

	@Override
	protected OrgContext getContext(ActionRequest request) {

		return new OrgContext(request);
	}

	@Override
	protected void exec(ActionRequest request, OrgContext paramContext) throws PortalException {

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		System.out.println("Starting to create " + paramContext.getNumberOfOrganizations() + " organizations");

		for (long i = paramContext.getStartIndex(); i <= paramContext.getNumberOfOrganizations(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfOrganizations());

			//Create Organization Name
			StringBundler organizationName = new StringBundler(2);
			
			//Only basename if creating Organization is only one.
			if(1 < paramContext.getNumberOfOrganizations()) {
				organizationName.append(i).append(paramContext.getBaseOrganizationName());
			} else {
				organizationName.append(paramContext.getBaseOrganizationName());
			}
			
			try {
				
				_organizationLocalService.addOrganization(
						paramContext.getServiceContext().getUserId(),
						paramContext.getParentOrganizationId(), // parentOrganizationId
						organizationName.toString(), // name
						paramContext.isOrganizationSiteCreate()); // site
				
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

		System.out.println("Finished creating " + paramContext.getNumberOfOrganizations() + " organizations");

	}

	@Reference
	private Portal _portal;
	
	@Reference
	private OrganizationLocalService _organizationLocalService;	
	
	private static final Log _log = LogFactoryUtil.getLog(
			OrgDefaultDummyGenerator.class);	
}
