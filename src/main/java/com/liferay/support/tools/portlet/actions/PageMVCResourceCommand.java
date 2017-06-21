package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Page lists action
 * 
 * @author yasuflatland
 *
 */
@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.CMD_PAGES_FOR_A_SITE
	},
	service = MVCResourceCommand.class
)
public class PageMVCResourceCommand extends BaseMVCResourceCommand {

	/**
	 * Commands
	 */
	static public final String CMD_PAGELIST = "pagelist";
	
	@Override
	protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws Exception {
		String cmd = ParamUtil.getString(resourceRequest, Constants.CMD);
		String serializedJson = "";
		
		if(cmd.equals(CMD_PAGELIST)) {
			serializedJson = getSiteLists(resourceRequest,resourceResponse );
		} else {
			_log.error("Unknown command is passed <" + cmd + ">");
		}
		
		HttpServletResponse response = _portal.getHttpServletResponse(
				resourceResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, serializedJson);		
	}
	
	/**
	 * Get Site lists
	 * 
	 * Depending on the passed site groupd id, fetch all pages in the site and return JSON object list.
	 * 
	 * @param resourceRequest
	 * @param resourceResponse
	 * @return
	 */
	protected String getSiteLists(ResourceRequest resourceRequest, ResourceResponse resourceResponse) {
		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
		long siteGroupId = ParamUtil.getLong(resourceRequest, "siteGroupId", themeDisplay.getSiteGroupId());
		
		List<Layout> layouts = _layoutLocalService.getLayouts(
				siteGroupId, 
				false, 
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, 
				false, 
				QueryUtil.ALL_POS, 
				QueryUtil.ALL_POS);
		
		for(Layout layout : layouts ) {
			JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();
			
			if(_log.isDebugEnabled()) {
				_log.debug("layout <" + layout.getName(themeDisplay.getLocale()) + ">");
				_log.debug(layout.toString());
				_log.debug("----------");
			}
			
			curUserJSONObject.put("name" , layout.getName(themeDisplay.getLocale()));
			curUserJSONObject.put("parentLayoutId" , layout.getLayoutId());
			
			jsonArray.put(curUserJSONObject);
		}
		
		return jsonArray.toJSONString();
	}

	@Reference
	private LayoutLocalService _layoutLocalService;		

	@Reference
	private Portal _portal;	
	
	private static final Log _log = LogFactoryUtil.getLog(
			PageMVCResourceCommand.class);		
}
