package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.List;
import java.util.stream.Collectors;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Page lists action
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.CMD_ROLELIST
	},
	service = MVCResourceCommand.class
)
public class RoleMVCResourceCommand extends BaseMVCResourceCommand {

	/**
	 * Commands
	 */
	static public final String CMD_ROLELIST = "rolelist";

	@Override
	protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse) throws Exception {
		String cmd = ParamUtil.getString(resourceRequest, Constants.CMD);
		String serializedJson = "";

		if (cmd.equals(CMD_ROLELIST)) {
			serializedJson = getRoleLists(resourceRequest, resourceResponse);
		} else {
			_log.error("Unknown command is passed <" + cmd + ">");
		}

		HttpServletResponse response = _portal.getHttpServletResponse(resourceResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, serializedJson);
	}

	/**
	 * Get Role lists 
	 * 
	 * This method filter roles 
	 * 
	 * @param resourceRequest
	 * @param resourceResponse
	 * @return filtered role list json strings
	 */
	protected String getRoleLists(ResourceRequest resourceRequest, ResourceResponse resourceResponse) {
		ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
		boolean isOrganizationsSelected = ParamUtil.getBoolean(resourceRequest, "isOrganizationSelected", false);
		boolean isSitesSelected = ParamUtil.getBoolean(resourceRequest, "isSitesSelected", false);

		List<Role> roles = _roleLocalService.getRoles(themeDisplay.getCompanyId());

		if (_log.isDebugEnabled()) {
	    	String roleids = roles.stream()
		      .map(r -> RoleConstants.getTypeLabel(r.getType()))
		      .collect(Collectors.joining(","));        	
	    	_log.debug("All role ids : " + roleids);
		}
    	
		for ( Role role : roles) {
			JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();

			if (role.getType() == RoleConstants.TYPE_SITE && false == isSitesSelected) {
				continue;
			} 
			if(role.getType() == RoleConstants.TYPE_ORGANIZATION && false == isOrganizationsSelected) {
				continue;
			}
		
			
			if (_log.isDebugEnabled()) {
				_log.debug("selected role <" + role.getTitle(themeDisplay.getLocale()) + ">");
				_log.debug(role.toString());
			}

			curUserJSONObject.put("name", role.getTitle(themeDisplay.getLocale()));
			curUserJSONObject.put("roleId", role.getRoleId());
			curUserJSONObject.put("type", role.getType());

			jsonArray.put(curUserJSONObject);
		}

		return jsonArray.toJSONString();
	}

	@Reference
	private Portal _portal;

	@Reference
	private RoleLocalService _roleLocalService;
	
	private static final Log _log = LogFactoryUtil.getLog(RoleMVCResourceCommand.class);

}
