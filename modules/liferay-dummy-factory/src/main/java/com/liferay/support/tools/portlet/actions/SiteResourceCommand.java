package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.SiteMembershipType;
import com.liferay.support.tools.service.usecase.SiteCreateUseCase;
import com.liferay.support.tools.service.usecase.SiteItemResult;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/site"
	},
	service = MVCResourceCommand.class
)
public class SiteResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create sites",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				SiteMembershipType membershipType =
					SiteMembershipType.fromString(
						GetterUtil.getString(
							data.getString("membershipType"), "open"));

				long parentGroupId = GetterUtil.getLong(
					data.getString("parentGroupId"));
				long siteTemplateId = GetterUtil.getLong(
					data.getString("siteTemplateId"));
				boolean manualMembership = GetterUtil.getBoolean(
					data.getString("manualMembership"), true);
				boolean inheritContent = GetterUtil.getBoolean(
					data.getString("inheritContent"));
				boolean active = GetterUtil.getBoolean(
					data.getString("active"), true);
				String description = data.getString("description");
				long publicLayoutSetPrototypeId = GetterUtil.getLong(
					data.getString("publicLayoutSetPrototypeId"));
				long privateLayoutSetPrototypeId = GetterUtil.getLong(
					data.getString("privateLayoutSetPrototypeId"));

				BatchResult<SiteItemResult> result = _siteCreateUseCase.create(
					context.getUserId(), context.getCompanyId(), batchSpec,
					membershipType, parentGroupId, siteTemplateId,
					manualMembership, inheritContent, active, description,
					publicLayoutSetPrototypeId, privateLayoutSetPrototypeId,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					item -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("groupId", item.groupId());
						json.put("name", item.name());
						json.put("inheritContent", item.inheritContent());
						json.put("parentGroupId", item.parentGroupId());

						if (item.publicLayoutSetPrototypeUuid() != null) {
							json.put(
								"publicLayoutSetPrototypeUuid",
								item.publicLayoutSetPrototypeUuid());
						}

						if (item.privateLayoutSetPrototypeUuid() != null) {
							json.put(
								"privateLayoutSetPrototypeUuid",
								item.privateLayoutSetPrototypeUuid());
						}

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private SiteCreateUseCase _siteCreateUseCase;

}
