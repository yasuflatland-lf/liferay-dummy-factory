package com.liferay.support.tools.portlet;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"com.liferay.portlet.add-default-resource=true",
		"com.liferay.portlet.css-class-wrapper=portlet-liferay-dummy-factory",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"javax.portlet.display-name=Liferay Dummy Factory",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.version=3.0"
	},
	service = Portlet.class
)
public class LiferayDummyFactoryPortlet extends MVCPortlet {
}
