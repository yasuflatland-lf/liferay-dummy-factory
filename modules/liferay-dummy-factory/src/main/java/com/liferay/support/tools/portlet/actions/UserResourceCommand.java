package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
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
import com.liferay.support.tools.service.EmailDomain;
import com.liferay.support.tools.service.UserBatchSpec;
import com.liferay.support.tools.service.usecase.UserCreateUseCase;
import com.liferay.support.tools.service.usecase.UserItemResult;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/user"
	},
	service = MVCResourceCommand.class
)
public class UserResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create users",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				UserBatchSpec userBatchSpec = new UserBatchSpec(
					batchSpec,
					EmailDomain.of(data.getString("emailDomain")),
					data.getString("password"),
					data.has("male") ? data.getBoolean("male") : true,
					data.getString("jobTitle"),
					_toLongArray(data.getJSONArray("organizationIds")),
					_toLongArray(data.getJSONArray("roleIds")),
					_toLongArray(data.getJSONArray("userGroupIds")),
					_toLongArray(data.getJSONArray("siteRoleIds")),
					_toLongArray(data.getJSONArray("orgRoleIds")),
					GetterUtil.getBoolean(
						data.getString("fakerEnable"), false),
					data.getString("locale"),
					GetterUtil.getBoolean(
						data.getString("generatePersonalSiteLayouts"), false),
					GetterUtil.getLong(
						data.getString("publicLayoutSetPrototypeId"), 0L),
					GetterUtil.getLong(
						data.getString("privateLayoutSetPrototypeId"), 0L),
					_toLongArray(data.getJSONArray("groupIds")));

				BatchResult<UserItemResult> result = _userCreateUseCase.create(
					context.getUserId(), context.getCompanyId(),
					userBatchSpec, context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					item -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("emailAddress", item.emailAddress());
						json.put("screenName", item.screenName());
						json.put("userId", item.userId());

						if (item.groupId() != 0L) {
							json.put("groupId", item.groupId());
						}

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

	private long[] _toLongArray(JSONArray jsonArray) {
		if ((jsonArray == null) || (jsonArray.length() == 0)) {
			return new long[0];
		}

		long[] result = new long[jsonArray.length()];

		for (int i = 0; i < jsonArray.length(); i++) {
			result[i] = GetterUtil.getLong(jsonArray.get(i));
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private UserCreateUseCase _userCreateUseCase;

}
