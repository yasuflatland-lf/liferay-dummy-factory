package com.liferay.support.tools.page;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Pages Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = PageDefaultDummyGenerator.class)
public class PageDefaultDummyGenerator extends DummyGenerator<PageContext> {

	@Override
	protected PageContext getContext(ActionRequest request) {

		return new PageContext(request);
	}

	@Override
	protected void exec(ActionRequest request, PageContext paramContext) throws PortalException {

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfpages() + " pages");

		for (long i = 1; i <= paramContext.getNumberOfpages(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfpages());
			
			//Create page name
			StringBundler name = new StringBundler(2);
			name.append(paramContext.getBasePageName());
			
			//Add number more then one Page
			if(1 < paramContext.getNumberOfpages()) {
				name.append(i);
			}

			try {
				_layoutLocalService.addLayout(
					StringPool.BLANK, //externalReferenceCode
					paramContext.getServiceContext().getUserId(), 
					paramContext.getGroupId(), //groupId
					paramContext.isPrivateLayout(), //privateLayout
					paramContext.getParentLayoutId(), //parentLayoutId
					name.toString(), //name
					StringPool.BLANK, //title
					StringPool.BLANK, //description
					paramContext.getLayoutType(), //type
					paramContext.isHidden(), //hidden
					StringPool.BLANK, //friendlyURL
					paramContext.getServiceContext()); //serviceContext
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
			
		}

		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfpages() + " pages");

	}

	@Reference
	private LayoutLocalService _layoutLocalService;	

}
