package com.liferay.support.tools.site;

import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Site Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = SiteDummyFactory.class)
public class SiteDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<SiteContext> create(ActionRequest request) {
		return _siteDefaultDummyGenerator;
	}

	@Reference
	SiteDefaultDummyGenerator _siteDefaultDummyGenerator;
	
}
