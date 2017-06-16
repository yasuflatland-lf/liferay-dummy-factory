package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.CommonUtil;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Yasuyuki Takeo
 */
@Component(
    immediate = true, 
    property = { 
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
        "mvc.command.name=" + LDFPortletKeys.ORGANIZAION,
        "mvc.command.name=" + LDFPortletKeys.SITES,
        "mvc.command.name=" + LDFPortletKeys.PAGES,
        "mvc.command.name=" + LDFPortletKeys.USERS,
        "mvc.command.name=" + LDFPortletKeys.COMMON
    }, 
    service = MVCRenderCommand.class
)
public class CommonMVCRenderCommand implements MVCRenderCommand {

	@Override
	public String render(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException {

		// Get Mode
		String mode = ParamUtil.getString(renderRequest, LDFPortletKeys.MODE,
				LDFPortletKeys.MODE_ORGANIZAION);
		renderRequest.setAttribute(LDFPortletKeys.MODE, mode);
		
		if(_log.isDebugEnabled()) {
			_log.debug("mode <" + mode + ">");
			_log.debug("jsp  <" + _commonUtil
			.getPageFromMode()
			.getOrDefault(mode, LDFPortletKeys.JSP_ORGANIZAION) + ">");
		}
		
		// Carry around mode
		renderRequest.setAttribute(LDFPortletKeys.MODE, mode);
		
		return _commonUtil
				.getPageFromMode()
				.getOrDefault(mode, LDFPortletKeys.JSP_ORGANIZAION);
	}

	@Reference(unbind = "-")
	public void setCommonUtil(CommonUtil commonUtil) {
		_commonUtil = commonUtil;
	}

	private CommonUtil _commonUtil;
	
	private static Log _log = LogFactoryUtil
			.getLog(CommonMVCRenderCommand.class);	
}
