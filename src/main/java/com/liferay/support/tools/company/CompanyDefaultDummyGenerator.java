package com.liferay.support.tools.company;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.instances.service.PortalInstancesLocalService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = CompanyDefaultDummyGenerator.class)
public class CompanyDefaultDummyGenerator extends DummyGenerator<CompanyContext> {

	@Override
	protected CompanyContext getContext(ActionRequest request) throws Exception {
		return new CompanyContext(request);
	}

	@Override
	protected void exec(ActionRequest request, CompanyContext paramContext)
		throws Exception {
		
		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfCompanies() + " companies");
				
		for (long i = 1; i <= paramContext.getNumberOfCompanies(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfCompanies());
			
			//Create company web id
			StringBundler webId = new StringBundler(2);
			webId.append(paramContext.getWebId());
			
			//Create company Virtual Host Name
			StringBundler virtualHostname = new StringBundler(2);
			virtualHostname.append(paramContext.getVirtualHostname());

			//Create company Mail Domain
			StringBundler mx = new StringBundler(2);
			mx.append(paramContext.getMx());

			//Add number more then one company
			if(1 < paramContext.getNumberOfCompanies()) {
				webId.append(i);
				virtualHostname.append(i);
				mx.append(i);
			}
			
			try {
				if(_log.isDebugEnabled()) {
					_log.debug("-----");
				}
				
				Company company = _companyLocalService.addCompany(
						webId.toString(), 
						virtualHostname.toString(), 
						mx.toString(), 
						paramContext.isSystem(),
						paramContext.getMaxUsers(), 
						paramContext.isActive()); 
				
				_portalInstancesLocalService.initializePortalInstance(
					paramContext.getServletContext(), company.getWebId());				
			
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}	
		}
		
		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfCompanies() + " companies");
		
	}

	@Reference
	private CompanyLocalService _companyLocalService;
	
	@Reference
	private PortalInstancesLocalService _portalInstancesLocalService;

	private static final Log _log = LogFactoryUtil.getLog(CompanyDefaultDummyGenerator.class);	
	
}