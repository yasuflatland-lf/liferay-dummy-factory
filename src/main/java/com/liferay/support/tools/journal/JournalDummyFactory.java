package com.liferay.support.tools.journal;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = JournalDummyFactory.class)
public class JournalDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<JournalContext> create(ActionRequest request) {
		long createContentsType = 0;
		DummyGenerator<JournalContext> generator = _JournalSimpleDummyGenerator;

		createContentsType = ParamUtil.getLong(request, "createContentsType", LDFPortletKeys.MB_CATEGORY_CREATE);

		if (createContentsType == LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) {
			generator = _JournalRandomDummyGenerator;
			
		} else if (createContentsType == LDFPortletKeys.WCM_STRUCTURE_TEMPLATE_SELECT_CREATE) {
			generator = _JournalStructureTemplateDummyGenerator;
			
		}
		
		return generator;
	}

	@Reference
	JournalSimpleDummyGenerator _JournalSimpleDummyGenerator;

	@Reference
	JournalRandomDummyGenerator _JournalRandomDummyGenerator;

	@Reference
	JournalStructureTemplateDummyGenerator _JournalStructureTemplateDummyGenerator;
}
