package com.liferay.support.tools.journal;

import com.google.common.collect.Maps;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.util.JournalConverter;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.DDMLocalUtil;

import java.io.Serializable;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Journal Articles Utilities
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = JournalUtils.class)
public class JournalUtils {

	/**
	 * Build content field
	 *
	 * @param groupId Company group ID
	 * @param locales locales
	 * @param baseArticle content data
	 * @return DDMStructure applied content XML strings
	 * @throws Exception
	 */
	public String buildFields(long groupId, String[] locales, String baseArticle) throws Exception {
		DDMStructure ddmStructure = _DDMStructureLocalService.getStructure(groupId,
				PortalUtil.getClassNameId(JournalArticle.class), LDFPortletKeys._DDM_STRUCTURE_KEY);

		Map<String, Serializable> fieldsMap = Maps.newConcurrentMap();
		fieldsMap.put(DDM_CONTENT, baseArticle);

		Fields fields = _ddmLocalUtil.toFields(ddmStructure.getStructureId(), fieldsMap, locales,
				LocaleUtil.getDefault());

		return _journalConverter.getContent(ddmStructure, fields);
	}

	@Reference
	private JournalConverter _journalConverter;

	@Reference
	private DDMStructureLocalService _DDMStructureLocalService;

	@Reference
	private DDMLocalUtil _ddmLocalUtil;

	private static final String DDM_CONTENT = "content";

}
