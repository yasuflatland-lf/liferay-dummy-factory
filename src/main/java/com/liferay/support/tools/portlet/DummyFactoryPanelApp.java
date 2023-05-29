package com.liferay.support.tools.portlet;

import com.liferay.application.list.BasePanelApp;
import com.liferay.application.list.PanelApp;
import com.liferay.application.list.constants.PanelCategoryKeys;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.support.tools.constants.LDFPortletKeys;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author yasuflatland
 */
@Component(
	immediate = true,
	property = {
		"panel.app.order:Integer=100",
		"panel.category.key=" + PanelCategoryKeys.CONTROL_PANEL_APPS
	},
	service = PanelApp.class
)
public class DummyFactoryPanelApp extends BasePanelApp {

	@Override
	public Portlet getPortlet() {
		return _portlet;
	}

	@Override
	public String getPortletId() {
		return LDFPortletKeys.LIFERAY_DUMMY_FACTORY;
	}

	@Reference(
		target = "(javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY + ")",
		unbind = "-"
	)
	private Portlet _portlet;
}
