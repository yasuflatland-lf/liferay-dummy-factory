package com.liferay.support.tools.document.library;

import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Document Library Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = DLDummyFactory.class)
public class DLDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<DLContext> create(ActionRequest request) {
		return _dlDefaultDummyGenerator;
	}

	@Reference
	DLDefaultDummyGenerator _dlDefaultDummyGenerator;	
}
