package com.liferay.support.tools.wiki;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = WikiDummyFactory.class)
public class WikiDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<WikiContext> create(ActionRequest request) {
		
		DummyGenerator<WikiContext> generator = _wikiNodeDummyGenerator;
		long createContentsType = ParamUtil.getLong(request, "createContentsType", LDFPortletKeys.W_NODE);

		if(_log.isDebugEnabled()) {
			_log.debug("mode : " + String.valueOf(createContentsType));
		}
		
		if (createContentsType == LDFPortletKeys.W_PAGE) {
			generator = _wikiPageDummyGenerator;
		}
		
		return generator;
	}

	@Reference
	private WikiNodeDummyGenerator _wikiNodeDummyGenerator;	

	@Reference
	private WikiPageDummyGenerator _wikiPageDummyGenerator;	

	private static final Log _log = LogFactoryUtil.getLog(WikiDummyFactory.class);	
}
