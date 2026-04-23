package com.liferay.support.tools.portlet.actions;

import com.liferay.asset.kernel.model.AssetCategory;
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
import com.liferay.support.tools.service.CategoryCreator;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/category"
	},
	service = MVCResourceCommand.class
)
public class CategoryResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletJsonCommandTemplate.serveJsonWithProgress(
			resourceRequest, resourceResponse, _portal, _log,
			"Failed to create categories",
			(context, data, responseJson) -> {
				BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

				long groupId = GetterUtil.getLong(data.getString("groupId"));
				long vocabularyId = GetterUtil.getLong(
					data.getString("vocabularyId"));

				ResourceCommandUtil.validatePositiveId(groupId, "groupId");
				ResourceCommandUtil.validatePositiveId(
					vocabularyId, "vocabularyId");

				BatchResult<AssetCategory> result = _categoryCreator.create(
					context.getUserId(), groupId, vocabularyId, batchSpec,
					context.getProgressCallback());

				return ResourceCommandUtil.toJson(
					result,
					category -> {
						JSONObject json = JSONFactoryUtil.createJSONObject();

						json.put("categoryId", category.getCategoryId());
						json.put("groupId", category.getGroupId());
						json.put("vocabularyId", category.getVocabularyId());
						json.put("name", category.getName());

						return json;
					});
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CategoryResourceCommand.class);

	@Reference
	private CategoryCreator _categoryCreator;

	@Reference
	private Portal _portal;

}
