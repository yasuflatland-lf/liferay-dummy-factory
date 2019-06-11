package com.liferay.support.tools.display.context;

import com.liferay.frontend.taglib.clay.servlet.taglib.util.NavigationItem;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.NavigationItemList;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.PortalPreferences;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;

import java.util.List;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;

/**
 * 
 * Dummy Factory Display Context
 * 
 * @author Yasuyuki Takeo
 *
 */
public class DummyFactoryDisplayContext {
	private HttpServletRequest		_request;
	private LiferayPortletRequest	_liferayPortletRequest;
	private LiferayPortletResponse	_liferayPortletResponse;
	private PortletPreferences		_portletPreferences;
	private PortalPreferences		_portalPreferences;

	public DummyFactoryDisplayContext(HttpServletRequest request,
			LiferayPortletRequest liferayPortletRequest,
			LiferayPortletResponse liferayPortletResponse,
			PortletPreferences portletPreferences) {

		_request = request;
		_liferayPortletRequest = liferayPortletRequest;
		_liferayPortletResponse = liferayPortletResponse;
		_portletPreferences = portletPreferences;

		_portalPreferences = PortletPreferencesFactoryUtil
				.getPortalPreferences( _request );
	}

	/**
	 * Get Navigation Bar Items
	 * 
	 * @param label
	 * @return NavigationItem List
	 */
	public List<NavigationItem> getNavigationBarItems( String label ) {
		return new NavigationItemList() {
			{
				add( navigationItem -> {
					navigationItem.setActive( true );
					navigationItem.setHref(
							_liferayPortletResponse.createRenderURL(),
							"currentPageName", label
					);
					navigationItem
							.setLabel( LanguageUtil.get( _request, label ) );
				});
			}
		};
	}
}
