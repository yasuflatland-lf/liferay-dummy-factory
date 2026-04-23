package com.liferay.support.tools.utils;

import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.util.JournalConverter;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Journal Articles Utilities
 *
 * @author Yasuyuki Takeo
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
	public String buildFields(long groupId, String[] locales, String baseArticle)
		throws Exception {

		DDMStructure ddmStructure = _ddmStructureLocalService.getStructure(
			groupId, PortalUtil.getClassNameId(JournalArticle.class),
			LDFPortletKeys.DDM_STRUCTURE_KEY, true);

		Map<String, Serializable> fieldsMap = new ConcurrentHashMap<>();
		fieldsMap.put(_DDM_CONTENT, baseArticle);

		Fields fields = _ddmLocalUtil.toFields(
			ddmStructure.getStructureId(), fieldsMap, locales,
			LocaleUtil.getDefault());

		return _journalConverter.getContent(ddmStructure, fields, groupId);
	}

	/**
	 * Build content field
	 *
	 * @param groupId Company group ID
	 * @param ddmStructure DDM structure
	 * @param locales locales
	 * @return DDMStructure applied content XML strings
	 * @throws Exception
	 */
	public String buildFields(
			long groupId, DDMStructure ddmStructure, String[] locales)
		throws Exception {

		Map<String, Serializable> fieldsMap = new ConcurrentHashMap<>();
		Set<String> fieldNames = ddmStructure.getFieldNames();

		for (String fieldName : fieldNames) {
			fieldsMap.put(fieldName, "");
		}

		Fields fields = _ddmLocalUtil.toFields(
			ddmStructure.getStructureId(), fieldsMap, locales,
			LocaleUtil.getDefault());

		return _journalConverter.getContent(ddmStructure, fields, groupId);
	}

	private static final String _DDM_CONTENT = "content";

	@Reference
	private DDMLocalUtil _ddmLocalUtil;

	@Reference
	private DDMStructureLocalService _ddmStructureLocalService;

	@Reference
	private JournalConverter _journalConverter;

}
