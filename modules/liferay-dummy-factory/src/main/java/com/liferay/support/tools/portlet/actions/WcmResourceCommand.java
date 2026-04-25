package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.AssetTagNames;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.WebContentBatchSpec;
import com.liferay.support.tools.service.WebContentCreator;
import com.liferay.support.tools.service.WebContentPerSiteResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/wcm"
	},
	service = MVCResourceCommand.class
)
public class WcmResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create web contents",
			(context, data, responseJson) -> {
				long[] groupIds = _parseGroupIds(data);

				if (groupIds.length == 0) {
					throw new IllegalArgumentException(
						"groupIds must contain at least one positive group id");
				}

				AssetTagNames tags = AssetTagNames.of(
					data.getString("tags"));

				WebContentBatchSpec spec = new WebContentBatchSpec(
					ResourceCommandUtil.parseBatchSpec(data),
					groupIds,
					GetterUtil.getLong(data.getString("folderId")),
					_parseLocales(data.getString("locales"), groupIds[0]),
					GetterUtil.getBoolean(
						data.getString("neverExpire"), true),
					GetterUtil.getBoolean(
						data.getString("neverReview"), true),
					GetterUtil.getInteger(
						data.getString("createContentsType")),
					GetterUtil.getString(data.getString("baseArticle")),
					GetterUtil.getInteger(
						data.getString("titleWords"), 5),
					GetterUtil.getInteger(
						data.getString("totalParagraphs"), 3),
					GetterUtil.getInteger(
						data.getString("randomAmount"), 3),
					GetterUtil.getString(data.getString("linkLists")),
					GetterUtil.getLong(data.getString("ddmStructureId")),
					GetterUtil.getLong(data.getString("ddmTemplateId")),
					tags);

				BatchResult<WebContentPerSiteResult> result =
					_webContentCreator.create(
						context.getUserId(), spec,
						context.getProgressCallback());

				JSONObject json = JSONFactoryUtil.createJSONObject();
				JSONArray perSiteArray = JSONFactoryUtil.createJSONArray();

				for (WebContentPerSiteResult perSite : result.items()) {
					JSONObject entry = JSONFactoryUtil.createJSONObject();

					entry.put("groupId", perSite.groupId());
					entry.put("siteName", perSite.siteName());
					entry.put("created", perSite.created());
					entry.put("failed", perSite.failed());

					if (perSite.error() != null) {
						entry.put("error", perSite.error());
					}

					perSiteArray.put(entry);
				}

				json.put("ok", result.success());
				json.put("totalRequested", result.requested());
				json.put("totalCreated", result.count());
				json.put("perSite", perSiteArray);

				if (!result.success()) {
					json.put("error", result.error());
				}

				return json;
			});
	}

	private long[] _parseGroupIds(JSONObject data) {
		JSONArray jsonArray = data.getJSONArray("groupIds");

		if ((jsonArray != null) && (jsonArray.length() > 0)) {
			List<Long> parsed = new ArrayList<>();

			for (int i = 0; i < jsonArray.length(); i++) {
				_addParsedGroupId(parsed, jsonArray.get(i));
			}

			return _toLongArray(parsed);
		}

		String groupIdsString = data.getString("groupIds");

		if (Validator.isNotNull(groupIdsString)) {
			List<Long> parsed = new ArrayList<>();

			for (String token : groupIdsString.split(",")) {
				if ((token != null) && !token.trim().isEmpty()) {
					_addParsedGroupId(parsed, token.trim());
				}
			}

			return _toLongArray(parsed);
		}

		long legacyGroupId = GetterUtil.getLong(data.getString("groupId"));

		if (legacyGroupId > 0) {
			return new long[] {legacyGroupId};
		}

		return new long[0];
	}

	private void _addParsedGroupId(List<Long> parsed, Object rawToken) {
		long value = GetterUtil.getLong(rawToken);

		if (value <= 0) {
			_log.warn(
				"Dropped unparseable or non-positive groupIds entry: " +
					rawToken);

			return;
		}

		parsed.add(value);
	}

	private String[] _parseLocales(String localesCsv, long groupId) {
		if (Validator.isNotNull(localesCsv)) {
			List<String> parsed = new ArrayList<>();

			for (String token : localesCsv.split(",")) {
				if ((token != null) && !token.trim().isEmpty()) {
					parsed.add(token.trim());
				}
			}

			if (!parsed.isEmpty()) {
				return parsed.toArray(new String[0]);
			}
		}

		try {
			Locale defaultLocale = _portal.getSiteDefaultLocale(groupId);

			return new String[] {LocaleUtil.toLanguageId(defaultLocale)};
		}
		catch (PortalException portalException) {
			_log.warn(
				"Unable to resolve site default locale for groupId " +
					groupId + ", falling back to portal default",
				portalException);

			return new String[] {
				LocaleUtil.toLanguageId(LocaleUtil.getDefault())
			};
		}
	}

	private long[] _toLongArray(List<Long> values) {
		long[] result = new long[values.size()];

		for (int i = 0; i < values.size(); i++) {
			result[i] = values.get(i);
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		WcmResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private WebContentCreator _webContentCreator;

}
