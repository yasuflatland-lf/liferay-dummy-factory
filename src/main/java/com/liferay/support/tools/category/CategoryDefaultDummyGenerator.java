package com.liferay.support.tools.category;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Category Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = CategoryDefaultDummyGenerator.class)
public class CategoryDefaultDummyGenerator extends DummyGenerator<CategoryContext> {

	@Override
	protected CategoryContext getContext(ActionRequest request) {

		return new CategoryContext(request);
	}

	@Override
	protected void exec(ActionRequest request, CategoryContext paramContext) throws PortalException {

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfCategories() + " categories");

		Map<Locale, String> descriptionMap = new ConcurrentHashMap<Locale, String>();
		descriptionMap.put(
			paramContext.getServiceContext().getLocale(), 
			"Sample Description"
		);
		
		for (long i = 1; i <= paramContext.getNumberOfCategories(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfCategories());
			
			//Create page name
			StringBundler title = new StringBundler(2);
			title.append(paramContext.getBaseCategoryName());
			
			//Add number more then one Page
			if(1 < paramContext.getNumberOfCategories()) {
				title.append(i);
			}

			Map<Locale, String> titleMap = new ConcurrentHashMap<Locale, String>();
			titleMap.put(
				paramContext.getServiceContext().getLocale(), 
				title.toString()
			);
			
			try {
				
				_assetCategoryLocalService.addCategory(
						paramContext.getServiceContext().getUserId(), // userId
						paramContext.getGroupId(), // groupId,
						paramContext.getParentCategoryId(), // parentCategoryId,
						titleMap, //titleMap, 
						descriptionMap, // descriptionMap,
						paramContext.getVocabularyId(), // vocabularyId
						null,
						paramContext.getServiceContext());
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
			
		}

		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfCategories() + " categories");

	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

}
