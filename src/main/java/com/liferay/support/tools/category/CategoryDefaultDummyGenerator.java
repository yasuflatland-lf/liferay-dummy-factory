package com.liferay.support.tools.category;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetCategoryService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
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

	protected String[] getCategoryProperties(ActionRequest actionRequest) {
		int[] categoryPropertiesIndexes = StringUtil.split(
			ParamUtil.getString(actionRequest, "categoryPropertiesIndexes"), 0);

		String[] categoryProperties =
			new String[categoryPropertiesIndexes.length];

		for (int i = 0; i < categoryPropertiesIndexes.length; i++) {
			int categoryPropertiesIndex = categoryPropertiesIndexes[i];

			String key = ParamUtil.getString(
				actionRequest, "key" + categoryPropertiesIndex);

			if (Validator.isNull(key)) {
				continue;
			}

			String value = ParamUtil.getString(
				actionRequest, "value" + categoryPropertiesIndex);

			categoryProperties[i] =
				key + AssetCategoryConstants.PROPERTY_KEY_VALUE_SEPARATOR +
					value;
		}

		return categoryProperties;
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
		
		ServiceContext serviceContext = ServiceContextFactory.getInstance(
				AssetCategory.class.getName(), request);
		
		String[] categoryProperties = getCategoryProperties(request);		
		
		for (long i = 1; i <= paramContext.getNumberOfCategories(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfCategories());
			
			//Create category name
			StringBundler title = new StringBundler(2);
			title.append(paramContext.getBaseCategoryName());
			
			//Add number more then one category
			if(1 < paramContext.getNumberOfCategories()) {
				title.append(i);
			}

			Map<Locale, String> titleMap = new ConcurrentHashMap<Locale, String>();
			titleMap.put(
				paramContext.getServiceContext().getLocale(), 
				title.toString()
			);
			
			try {
				
				if(_log.isDebugEnabled()) {
					_log.debug("-----");
					_log.debug("User  ID : " + String.valueOf(paramContext.getServiceContext().getUserId()));
					_log.debug("Group ID : " + String.valueOf(paramContext.getGroupId()));
					_log.debug("Parent CategoryId ID : " + String.valueOf(paramContext.getParentCategoryId()));
					_log.debug("Vocabulary ID : " + String.valueOf(paramContext.getVocabularyId()));
				}
				
				_assetCategoryService.addCategory(
						paramContext.getGroupId(), 
						paramContext.getParentCategoryId(),
						titleMap,
						descriptionMap, 
						paramContext.getVocabularyId(), 
						categoryProperties,
						serviceContext);
				
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
	
	@Reference
	private AssetCategoryService _assetCategoryService;

	private static final Log _log = LogFactoryUtil.getLog(CategoryDefaultDummyGenerator.class);	

}
