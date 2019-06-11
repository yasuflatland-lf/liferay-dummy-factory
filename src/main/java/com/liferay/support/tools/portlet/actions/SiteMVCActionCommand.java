package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.site.SiteContext;
import com.liferay.support.tools.site.SiteDummyFactory;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.MutableRenderParameters;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Sites
 * 
 * @author Yasuyuki Takeo
 */
@Component(
	immediate = true, 
	property = { 
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.SITES
	}, 
	service = MVCActionCommand.class
)
public class SiteMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest request, ActionResponse response) {

		try {
			DummyGenerator<SiteContext> dummyGenerator = _siteDummyFactory.create(request);
			dummyGenerator.create(request);

		} catch (Exception e) {
			hideDefaultSuccessMessage(request);
			_log.error(e, e);
		}

		MutableRenderParameters mutableRenderParameters = response.getRenderParameters();
		mutableRenderParameters.setValues("mvcRenderCommandName", LDFPortletKeys.COMMON);
		SessionMessages.add(request, "success");		
	}
	
	@Reference
	SiteDummyFactory _siteDummyFactory;
	
	private static final Log _log = LogFactoryUtil.getLog(
			SiteMVCActionCommand.class);		
}
