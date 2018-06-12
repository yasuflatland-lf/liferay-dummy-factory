package com.liferay.support.tools.wiki;

import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = WikiDefaultDummyGenerator.class)
public class WikiDefaultDummyGenerator extends DummyGenerator<WikiContext> {

	@Override
	protected WikiContext getContext(ActionRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void exec(ActionRequest request, WikiContext paramContext) throws Exception {
		// TODO Auto-generated method stub

	}

}
