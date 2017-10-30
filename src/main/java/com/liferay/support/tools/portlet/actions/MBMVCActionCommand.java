package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.*;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.messageboard.MBContext;
import com.liferay.support.tools.messageboard.MBDummyFactory;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Message Board
 * 
 * @author Yasuyuki Takeo
 */
@Component(
	immediate = true, 
	property = { 
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.MB
	}, 
	service = MVCActionCommand.class
)
public class MBMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest request, ActionResponse response) throws Exception {

		try {
			response.setRenderParameter("mvcRenderCommandName", LDFPortletKeys.COMMON);

			DummyGenerator<MBContext> dummyGenerator = _MBDummyFactory.create(request);
			dummyGenerator.create(request);

			SessionMessages.add(request, "success");

		} catch (Exception e) {
			hideDefaultSuccessMessage(request);
			SessionErrors.add(request,Exception.class);
			_log.error(e, e);
		}

	}

	@Reference
	MBDummyFactory _MBDummyFactory;
	
	private static final Log _log = LogFactoryUtil.getLog(MBMVCActionCommand.class);
}
