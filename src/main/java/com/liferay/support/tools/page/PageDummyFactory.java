package com.liferay.support.tools.page;

import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Pages Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = PageDummyFactory.class)
public class PageDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<PageContext> create(ActionRequest request) {
		return _pageDefaultDummyGenerator;
	}

	@Reference
	PageDefaultDummyGenerator _pageDefaultDummyGenerator;
	
}
