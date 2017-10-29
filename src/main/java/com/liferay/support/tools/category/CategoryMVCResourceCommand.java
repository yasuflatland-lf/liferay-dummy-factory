package com.liferay.support.tools.category;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Category resources
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.CMD_CATEGORY_LIST
	},
	service = MVCResourceCommand.class
)
public class CategoryMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws Exception {
		String serializedJson = "";

		serializedJson = getCategoryList(resourceRequest, resourceResponse);

		HttpServletResponse response = _portal.getHttpServletResponse(resourceResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, serializedJson);
	}

	/**
	 * Get Category list
	 * 
	 * @param resourceRequest
	 * @param resourceResponse
	 * @return
	 * @throws PortalException
	 */
	private String getCategoryList(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws PortalException {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long vocabularyId = ParamUtil.getLong(resourceRequest, "vocabularyId",0);
		
		if (_log.isDebugEnabled()) {
			_log.debug("Vocabulary Id <" + String.valueOf(vocabularyId) + ">");
		}

		List<AssetCategory> categories = 
			_assetCategoryLocalService.getVocabularyCategories(
				vocabularyId,
				QueryUtil.ALL_POS,
				QueryUtil.ALL_POS,
				null
			);

		for (AssetCategory category : categories) {
			JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();

			if (_log.isDebugEnabled()) {
				_log.debug("Category name <" + category.getName() + ">");
				_log.debug(category.toString());
				_log.debug("----------");
			}

			curUserJSONObject.put("categoryId", category.getCategoryId());
			curUserJSONObject.put("categoryName", category.getName());

			jsonArray.put(curUserJSONObject);
		}

		return jsonArray.toJSONString();
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Reference
	private Portal _portal;

	private static final Log _log = LogFactoryUtil.getLog(CategoryMVCResourceCommand.class);
}
