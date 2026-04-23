package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.RoleCreator;
import com.liferay.support.tools.service.RoleType;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/role"
	},
	service = MVCResourceCommand.class
)
public class RoleResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create roles",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				RoleType roleType = RoleType.fromString(
					GetterUtil.getString(data.getString("roleType"), "regular"));
				String description = GetterUtil.getString(
					data.getString("description"), "");

				BatchResult<Role> result = _roleCreator.create(
					context.getUserId(), batchSpec, roleType, description,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					role -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("name", role.getName());
						json.put("roleId", role.getRoleId());
						json.put("type", role.getType());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RoleResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private RoleCreator _roleCreator;

}
