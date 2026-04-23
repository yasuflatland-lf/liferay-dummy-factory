package com.liferay.support.tools.service.datalist;

import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class DDMTemplatesDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long journalClassNameId = PortalUtil.getClassNameId(
			JournalArticle.class);

		List<DDMStructure> structures =
			_ddmStructureLocalService.getClassStructures(
				companyId, journalClassNameId);

		for (DDMStructure structure : structures) {
			List<DDMTemplate> templates = _ddmTemplateLocalService.getTemplates(
				structure.getStructureId());

			for (DDMTemplate template : templates) {
				jsonArray.put(
					createOption(
						template.getName(LocaleUtil.getDefault()),
						template.getTemplateId()));
			}
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"ddm-templates"};
	}

	@Reference
	private DDMStructureLocalService _ddmStructureLocalService;

	@Reference
	private DDMTemplateLocalService _ddmTemplateLocalService;

}
