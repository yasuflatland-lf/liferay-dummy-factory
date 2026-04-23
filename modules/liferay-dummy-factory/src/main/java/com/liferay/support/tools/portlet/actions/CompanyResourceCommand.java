package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.CompanyCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/company"
	},
	service = MVCResourceCommand.class
)
public class CompanyResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create companies",
			(context, data, responseJson) -> {
				int count = GetterUtil.getInteger(data.getString("count"));
				String webId = GetterUtil.getString(data.getString("webId"));
				String virtualHostname = GetterUtil.getString(
					data.getString("virtualHostname"));
				String mx = GetterUtil.getString(data.getString("mx"));
				int maxUsers = GetterUtil.getInteger(
					data.getString("maxUsers"), 0);
				boolean active = GetterUtil.getBoolean(
					data.getString("active"), true);

				ResourceCommandUtil.validateCount(count);
				ResourceCommandUtil.validateNotEmpty(webId, "webId");
				ResourceCommandUtil.validateNotEmpty(
					virtualHostname, "virtualHostname");
				ResourceCommandUtil.validateNotEmpty(mx, "mx");

				BatchResult<Company> result = _companyCreator.create(
					count, webId, virtualHostname, mx, maxUsers, active,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					company -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("companyId", company.getCompanyId());
						json.put("webId", company.getWebId());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CompanyResourceCommand.class);

	@Reference
	private CompanyCreator _companyCreator;

	@Reference
	private Portal _portal;

}
