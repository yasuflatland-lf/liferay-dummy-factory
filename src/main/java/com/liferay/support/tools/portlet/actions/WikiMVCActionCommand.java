package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.wiki.WikiContext;
import com.liferay.support.tools.wiki.WikiDummyFactory;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Wiki nodes
 *
 * @author Yasuyuki Takeo
 */
@Component(
    immediate = true,
    property = {
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
        "mvc.command.name=" + LDFPortletKeys.WIKI
    },
    service = MVCActionCommand.class
)
public class WikiMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest request, ActionResponse response) {

		try {
			DummyGenerator<WikiContext> dummyGenerator = _wikiDummyFactory.create(request);
			dummyGenerator.create(request);

		} catch (Exception e) {
			hideDefaultSuccessMessage(request);
			_log.error(e, e);
			return;
		}

		response.setRenderParameter("mvcRenderCommandName", LDFPortletKeys.COMMON);
		SessionMessages.add(request, "success");

	}

	@Reference
	private WikiDummyFactory _wikiDummyFactory;

	private static final Log _log = LogFactoryUtil.getLog(WikiMVCActionCommand.class);
}
