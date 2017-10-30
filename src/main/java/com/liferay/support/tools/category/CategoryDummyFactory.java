package com.liferay.support.tools.category;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Category / Vocabulary Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = CategoryDummyFactory.class)
public class CategoryDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<CategoryContext> create(ActionRequest request) {
		
		long createContentsType = 0;
		DummyGenerator<CategoryContext> generator = _categoryDefaultDummyGenerator;

		createContentsType = ParamUtil.getLong(request, "createContentsType", LDFPortletKeys.C_CATEGORY_CREATE);

		if (createContentsType == LDFPortletKeys.C_VOCABULARY_CREATE) {
			generator = _vocabularyDefaultDummyGenerator;
		}
		
		return generator;
	}

	@Reference
	CategoryDefaultDummyGenerator _categoryDefaultDummyGenerator;
	
	@Reference
	VocabularyDefaultDummyGenerator _vocabularyDefaultDummyGenerator;
}
