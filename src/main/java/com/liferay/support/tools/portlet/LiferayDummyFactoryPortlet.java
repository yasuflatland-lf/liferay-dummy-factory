package com.liferay.support.tools.portlet;

import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.portlet.actions.DummyFactoryConfiguration;
import com.liferay.support.tools.utils.LodashResolver;

import java.io.IOException;
import java.util.Map;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * Dummy Factory Portlet
 *
 * @author Yasuyuki Takeo
 *
 */
@Component(
    immediate = true,
    configurationPid = LDFPortletKeys.DUMMY_FACTORY_CONFIG,
    configurationPolicy = ConfigurationPolicy.OPTIONAL,
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
    @Override
    public void doView(RenderRequest renderRequest, RenderResponse renderResponse)
        throws IOException, PortletException {

        renderRequest.setAttribute(DummyFactoryConfiguration.class.getName(), _dummyFactoryConfiguration);
        
        //Loading Lodash
        LodashResolver.exec(renderRequest, _npmResolver);
        
        super.doView(renderRequest, renderResponse);
    }

	@Override
	protected void doDispatch(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		renderRequest.setAttribute(DummyFactoryConfiguration.class.getName(), _dummyFactoryConfiguration);

		super.doDispatch(renderRequest, renderResponse);
	}
	
    @Activate
    @Modified
    protected void activate(Map<Object, Object> properties) {

		_dummyFactoryConfiguration = ConfigurableUtil.createConfigurable(DummyFactoryConfiguration.class, properties);
    }

    @Reference
    private NPMResolver _npmResolver;
    private volatile DummyFactoryConfiguration _dummyFactoryConfiguration;
    
}