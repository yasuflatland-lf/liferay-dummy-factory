package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.OrganizationCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/org"
	},
	service = MVCResourceCommand.class
)
public class OrganizationResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create organizations",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long parentOrganizationId = GetterUtil.getLong(
					data.getString("parentOrganizationId"));
				boolean site = GetterUtil.getBoolean(data.getString("site"));

				BatchResult<Organization> result =
					_organizationCreator.create(
						context.getUserId(), batchSpec, parentOrganizationId,
						site, context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					organization -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("name", organization.getName());
						json.put(
							"organizationId",
							organization.getOrganizationId());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OrganizationResourceCommand.class);

	@Reference
	private OrganizationCreator _organizationCreator;

	@Reference
	private Portal _portal;

}
