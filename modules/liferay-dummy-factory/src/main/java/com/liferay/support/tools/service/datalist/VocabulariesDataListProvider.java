package com.liferay.support.tools.service.datalist;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class VocabulariesDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		return JSONFactoryUtil.createJSONArray();
	}

	@Override
	public JSONArray getOptions(
			long companyId, String type,
			HttpServletRequest httpServletRequest)
		throws Exception {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long groupId = ParamUtil.getLong(httpServletRequest, "groupId", 0);

		if (groupId <= 0) {
			return jsonArray;
		}

		List<AssetVocabulary> vocabularies =
			_assetVocabularyLocalService.getGroupVocabularies(groupId);

		for (AssetVocabulary vocabulary : vocabularies) {
			jsonArray.put(
				createOption(
					vocabulary.getTitle(LocaleUtil.getDefault()),
					vocabulary.getVocabularyId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"vocabularies"};
	}

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

}
