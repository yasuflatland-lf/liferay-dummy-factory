package com.liferay.support.tools.category;

import com.liferay.asset.kernel.exception.DuplicateVocabularyException;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Vocabulary Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = VocabularyDefaultDummyGenerator.class)
public class VocabularyDefaultDummyGenerator extends DummyGenerator<CategoryContext> {

	@Override
	protected CategoryContext getContext(ActionRequest request) {

		return new CategoryContext(request);
	}

	@Override
	protected void exec(ActionRequest request, CategoryContext paramContext) throws PortalException {

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfVocabulary() + " vocabularies");

		for (long i = 1; i <= paramContext.getNumberOfVocabulary(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfVocabulary());
			
			//Create page name
			StringBundler title = new StringBundler(2);
			title.append(paramContext.getBaseVocabularyName());
			
			//Add number more then one Page
			if(1 < paramContext.getNumberOfVocabulary()) {
				title.append(i);
			}

			try {
				_assetVocabularyLocalService.addVocabulary(
					paramContext.getServiceContext().getUserId(), // userId
					paramContext.getGroupId(), //groupId
					title.toString(), // title
					paramContext.getServiceContext()); //serviceContext
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				
				if(e instanceof DuplicateVocabularyException) {
					_log.error("Vocabulary name has been duplicated. Please use different name.");
				} else {
					throw e;
				}
			}
			
		}

		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfVocabulary() + " vocabularies");

	}

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;
	
	private static final Log _log = LogFactoryUtil.getLog(VocabularyDefaultDummyGenerator.class);
}
