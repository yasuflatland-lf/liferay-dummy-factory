package com.liferay.support.tools.portlet;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.css-class-wrapper=dummy-factory",
		"com.liferay.portlet.display-category=category.tools",
		"com.liferay.portlet.preferences-owned-by-group=true",
		"com.liferay.portlet.preferences-unique-per-layout=false",
		"com.liferay.portlet.private-request-attributes=false",
		"com.liferay.portlet.private-session-attributes=false",
		"com.liferay.portlet.scopeable=true",
		"com.liferay.portlet.struts-path=dummy_factory",
		"com.liferay.portlet.use-default-template=true",
		"javax.portlet.display-name=Dummy Factory",
		"javax.portlet.expiration-cache=0",
		"javax.portlet.init-param.mvc-command-names-default-views=/ldf/org",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=administrator,power-user,user",
		"javax.portlet.supports.mime-type=text/html"
	},
	service = Portlet.class
)
public class LiferayDummyFactoryPortlet extends MVCPortlet {
}