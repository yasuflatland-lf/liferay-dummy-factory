package com.liferay.support.tools.portlet;

import com.liferay.portal.kernel.resource.bundle.ResourceBundleLoader;
import com.liferay.portal.kernel.resource.bundle.ResourceBundleLoaderUtil;
import com.liferay.portal.kernel.util.AggregateResourceBundle;

import java.util.Locale;
import java.util.ResourceBundle;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"bundle.symbolic.name=liferay.dummy.factory",
		"resource.bundle.base.name=content.Language",
		"servlet.context.name=liferay-dummy-factory"
	},
	service = ResourceBundleLoader.class
)
public class LDFResourceBundleLoader implements ResourceBundleLoader {

	@Override
	public ResourceBundle loadResourceBundle(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundle.getBundle(
			_BASE_NAME, locale, LDFResourceBundleLoader.class.getClassLoader());

		ResourceBundleLoader portalResourceBundleLoader =
			ResourceBundleLoaderUtil.getPortalResourceBundleLoader();

		ResourceBundle portalResourceBundle =
			portalResourceBundleLoader.loadResourceBundle(locale);

		return new AggregateResourceBundle(
			resourceBundle, portalResourceBundle);
	}

	private static final String _BASE_NAME = "content.Language";

}
