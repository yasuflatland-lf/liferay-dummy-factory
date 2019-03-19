package com.liferay.support.tools.company;

import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Blogs post dummy factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component( immediate = true, service = CompanyDummyFactory.class )
public class CompanyDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<CompanyContext> create( ActionRequest request ) {
		return _companyDefaultDummyGenerator;
	}

	@Reference
	CompanyDefaultDummyGenerator _companyDefaultDummyGenerator;
}
