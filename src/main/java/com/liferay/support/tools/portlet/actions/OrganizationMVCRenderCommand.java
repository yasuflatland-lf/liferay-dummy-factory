package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.support.tools.constants.LiferayDummyFactoryPortletKeys;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;

/**
 * @author Yasuyuki Takeo
 */
@Component(
		immediate = true, 
		property = { 
			"javax.portlet.name=" + LiferayDummyFactoryPortletKeys.LIFERAY_DUMMY_FACTORY,
			"mvc.command.name=" + LiferayDummyFactoryPortletKeys.ORGANIZAION 
		}, 
		service = MVCRenderCommand.class
	)
public class OrganizationMVCRenderCommand implements MVCRenderCommand {

	private static final String renderJSP = "view.jsp";
	
	@Override
	public String render(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException {
		return renderJSP;
	}

}
