package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
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
		"mvc.command.name=" + LDFPortletKeys.CMD_ROLELIST
	},
	service = MVCActionCommand.class
)
public class RoleMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Commands
	 */
	static public final String CMD_ROLELIST = "rolelist";

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);
		String serializedJson = "";

		if (cmd.equals(CMD_ROLELIST)) {
			serializedJson = getRoleLists(actionRequest, actionResponse);
		} else {
			_log.error("Unknown command is passed <" + cmd + ">");
		}

		HttpServletResponse response = _portal.getHttpServletResponse(actionResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, serializedJson);
	}

	/**
	 * Get Role lists 
	 * 
	 * This method filter roles 
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @return filtered role list json strings
	 */
	protected String getRoleLists(ActionRequest actionRequest, ActionResponse actionResponse) {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
		boolean isOrganizationsSelected = ParamUtil.getBoolean(actionRequest, "isOrganizationSelected", false);
		boolean isSitesSelected = ParamUtil.getBoolean(actionRequest, "isSitesSelected", false);

		List<Role> roles = _roleLocalService.getRoles(themeDisplay.getCompanyId());

		for ( Role role : roles) {
			JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();

			if(role.getType() == RoleConstants.TYPE_ORGANIZATION && false == isOrganizationsSelected) {
				continue;
			}
			if (role.getType() == RoleConstants.TYPE_SITE && false == isSitesSelected) {
				continue;
			} 
			
			if (_log.isDebugEnabled()) {
				_log.debug("role <" + role.getTitle(themeDisplay.getLocale()) + ">");
				_log.debug(role.toString());
				_log.debug("----------");
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
	
	private static final Log _log = LogFactoryUtil.getLog(RoleMVCActionCommand.class);
}
