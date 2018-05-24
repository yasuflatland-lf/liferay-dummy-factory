package com.liferay.support.tools.blogs;

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
@Component(immediate = true, service = BlogsDummyFactory.class)
public class BlogsDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<BlogsContext> create(ActionRequest request) {
		return _blogsDefaultDummyGenerator;
	}

	@Reference
	BlogsDefaultDummyGenerator _blogsDefaultDummyGenerator;	
}
