package com.liferay.support.tools.portlet.actions;

import com.liferay.asset.kernel.model.AssetVocabulary;
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
import com.liferay.support.tools.service.VocabularyCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/vocabulary"
	},
	service = MVCResourceCommand.class
)
public class VocabularyResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create vocabularies",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));

				ResourceCommandUtil.validatePositiveId(groupId, "groupId");

				BatchResult<AssetVocabulary> result = _vocabularyCreator.create(
					context.getUserId(), groupId, batchSpec,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					vocabulary -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("vocabularyId", vocabulary.getVocabularyId());
						json.put("groupId", vocabulary.getGroupId());
						json.put("name", vocabulary.getName());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		VocabularyResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private VocabularyCreator _vocabularyCreator;

}
