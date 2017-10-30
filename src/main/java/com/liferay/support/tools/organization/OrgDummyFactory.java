package com.liferay.support.tools.organization;

import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Organization Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = OrgDummyFactory.class)
public class OrgDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<OrgContext> create(ActionRequest request) {
		return _orgDefaultDummyGenerator;
	}

	@Reference
	OrgDefaultDummyGenerator _orgDefaultDummyGenerator;
	
}
