package com.liferay.support.tools.messageboard;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Message Board Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = MBDummyFactory.class)
public class MBDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<MBContext> create(ActionRequest request) {
		long createContentsType = 0;
		DummyGenerator<MBContext> generator = _MBThreadDummyGenerator;

		createContentsType = ParamUtil.getLong(request, "createContentsType", LDFPortletKeys.MB_CATEGORY_CREATE);

		if (createContentsType == LDFPortletKeys.MB_CATEGORY_CREATE) {
			generator = _MBCategoryDummyGenerator;
		} else if(createContentsType == LDFPortletKeys.MB_REPLY_CREATE) {
			generator = _MBReplyDummyGenerator;
		}
		
		return generator;
	}

	@Reference
	MBCategoryDummyGenerator _MBCategoryDummyGenerator;
	
	@Reference
	MBThreadDummyGenerator _MBThreadDummyGenerator;
	
	@Reference
	MBReplyDummyGenerator _MBReplyDummyGenerator;	
}
