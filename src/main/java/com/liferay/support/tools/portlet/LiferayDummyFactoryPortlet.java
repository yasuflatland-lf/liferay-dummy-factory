package com.liferay.support.tools.portlet;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.display-category=category.tools",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.display-name=Dummy Factory",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=administrator,power-user,user",
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY
	},
	service = Portlet.class
)
public class LiferayDummyFactoryPortlet extends MVCPortlet {
}