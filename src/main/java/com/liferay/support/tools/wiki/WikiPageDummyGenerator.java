package com.liferay.support.tools.wiki;

import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;
import com.liferay.wiki.service.WikiPageLocalService;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = WikiPageDummyGenerator.class)
public class WikiPageDummyGenerator extends DummyGenerator<WikiContext> {

	@Override
	protected WikiContext getContext(ActionRequest request) throws Exception {
		return new WikiContext(request);
	}

	@Override
	protected void exec(ActionRequest request, WikiContext paramContext) throws Exception {
		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfnodes() + " pages");

		for (long i = 1; i <= paramContext.getNumberOfnodes(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfnodes());
			
			//Create page name
			StringBundler name = new StringBundler(2);
			name.append(paramContext.getBaseNodeName());
			
			//Add number more then one Page
			if(1 < paramContext.getNumberOfnodes()) {
				name.append(i);
			}

			try {
				_wikiPageLocalService.addPage(
					paramContext.getServiceContext().getUserId(), 
					0, //noddId
					"", //title
					"", //content
					"", //summary
					true,//minorEdit
					paramContext.getServiceContext()); //serviceContext
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
			
		}

		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfnodes() + " pages");		
		
	}

	@Reference
	private WikiPageLocalService _wikiPageLocalService;
}
