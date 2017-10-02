package com.liferay.support.tools.portlet.actions;

import com.google.common.collect.Lists;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.ConfigurationAction;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import aQute.bnd.annotation.metatype.Configurable;

/**
 * Dummy Factory Configuraion Aciton
 * 
 * @author Yasuyuki Takeo
 * @author yasuflatland
 *
 */
@Component(
    immediate = true,
    configurationPid = LDFPortletKeys.DUMMY_FACTORY_CONFIG,
    configurationPolicy = ConfigurationPolicy.OPTIONAL,
    property = {
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
    }, service = ConfigurationAction.class
)
public class DummyFactoryConfigurationAction extends DefaultConfigurationAction {
	@Override
	public String getJspPath(HttpServletRequest httpServletRequest) {
		return "/configuration.jsp";
	}

	@Override
	public void processAction(PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse)
			throws Exception {

		String linkList = ParamUtil.getString(actionRequest, DummyFactoryConfiguration.CONF_LINKLIST);
		String urlList = ParamUtil.getString(actionRequest, DummyFactoryConfiguration.CONF_URLLIST);
		
		if (_log.isDebugEnabled()) {
			_log.debug("Link List :" + linkList);
			_log.debug("URL List  :" + urlList);
		}

		List<String> errors = Lists.newArrayList();
		if (validate(linkList, errors)) {
			setPreference(actionRequest, DummyFactoryConfiguration.CONF_LINKLIST, linkList);
			setPreference(actionRequest, DummyFactoryConfiguration.CONF_URLLIST, urlList);

			SessionMessages.add(actionRequest, "prefs-success");
		}

		super.processAction(portletConfig, actionRequest, actionResponse);
	}

	@Override
	public void include(PortletConfig portletConfig, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws Exception {

		if (_log.isDebugEnabled()) {
			_log.debug("Dummy Factory Portlet configuration include");
		}

		httpServletRequest.setAttribute(DummyFactoryConfiguration.class.getName(), _DummyFactoryConfiguration);

		super.include(portletConfig, httpServletRequest, httpServletResponse);
	}

	@Activate
	@Modified
	protected void activate(Map<Object, Object> properties) {
		_DummyFactoryConfiguration = Configurable.createConfigurable(DummyFactoryConfiguration.class, properties);
	}

	/**
	 * Validate Preference
	 *
	 * @param linkList String Link list
	 * @param errors
	 * @return boolean
	 */
	protected boolean validate(String linkList, List<String> errors) {
		boolean valid = true;

		if (Validator.isNotNull(linkList)) {

			String urls[] = linkList.split(",");
			
			if(0 == urls.length) {
				errors.add("Link list has to be split by comma");
				valid = false;
			}
		}
		
		return valid;
	}

	private static final Log _log = LogFactoryUtil.getLog(DummyFactoryConfigurationAction.class);

	private volatile DummyFactoryConfiguration _DummyFactoryConfiguration;

}
