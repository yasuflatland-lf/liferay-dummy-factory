package com.liferay.support.tools.utils;

import com.liferay.dynamic.data.mapping.storage.Field;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * DDM Structure handl utilities
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(service = DDMLocalUtil.class)
public class DDMLocalUtil {
	
	/**
	 * Get serializable values 
	 * 
	 * Customized version of DDLRecordLocalServiceImpl#_getSerializableValues
	 * 
	 * @see com.liferay.dynamic.data.lists.service.impl.DDLRecordLocalServiceImpl
	 * @param value value to put into structure.
	 * @return Serializable value 
	 */
	protected List<Serializable> _getSerializableValues(Serializable value) {
		List<Serializable> serializableValues = null;

		if (value instanceof Collection) {
			Collection<Serializable> values = (Collection<Serializable>) value;

			serializableValues = new ArrayList<>(values);
		} else if (value instanceof Serializable[]) {
			Serializable[] values = (Serializable[]) value;

			serializableValues = ListUtil.toList(values);
		} else if (value instanceof boolean[]) {
			boolean[] values = (boolean[]) value;

			serializableValues = new ArrayList<>(values.length);

			for (boolean serializableValue : values) {
				serializableValues.add(serializableValue);
			}
		} else if (value instanceof double[]) {
			double[] values = (double[]) value;

			serializableValues = new ArrayList<>(values.length);

			for (double serializableValue : values) {
				serializableValues.add(serializableValue);
			}
		} else if (value instanceof float[]) {
			float[] values = (float[]) value;

			serializableValues = new ArrayList<>(values.length);

			for (float serializableValue : values) {
				serializableValues.add(serializableValue);
			}
		} else if (value instanceof int[]) {
			int[] values = (int[]) value;

			serializableValues = new ArrayList<>(values.length);

			for (int serializableValue : values) {
				serializableValues.add(serializableValue);
			}
		} else if (value instanceof long[]) {
			long[] values = (long[]) value;

			serializableValues = new ArrayList<>(values.length);

			for (long serializableValue : values) {
				serializableValues.add(serializableValue);
			}
		} else if (value instanceof short[]) {
			short[] values = (short[]) value;

			serializableValues = new ArrayList<>(values.length);

			for (short serializableValue : values) {
				serializableValues.add(serializableValue);
			}
		}

		return serializableValues;
	}

	/**
	 * Create fields for DDMStructure
	 * 
	 * Customized version for DDLRecordLocalServiceImpl#toFields
	 * 
	 * @param ddmStructureId 
	 * @param fieldsMap Map for name and value in fields
	 * @param languageIds Multiple language IDs
	 * @param defaultLocale Default Locale object
	 * @see com.liferay.dynamic.data.lists.service.impl.DDLRecordLocalServiceImpl
	 * @return Fields for DDMStructure
	 */
	public Fields toFields(long ddmStructureId, Map<String, Serializable> fieldsMap, String[] languageIds,
			Locale defaultLocale) {

		Fields fields = new Fields();

		for (Map.Entry<String, Serializable> entry : fieldsMap.entrySet()) {
			Field field = new Field();

			field.setDDMStructureId(ddmStructureId);
			field.setName(entry.getKey());

			Serializable value = entry.getValue();

			List<Serializable> serializableValues = _getSerializableValues(value);

			for(String languageId : languageIds ) {
				if (serializableValues != null) {
					Locale locale = LocaleUtil.fromLanguageId(languageId);
					field.addValues(locale, serializableValues);
				}
				else {
					Locale locale = LocaleUtil.fromLanguageId(languageId);
					field.addValue(locale, value);
				}					
			}
			
			field.setDefaultLocale(defaultLocale);

			fields.put(field);
		}

		Field fieldsDisplayField = fields.get(_FIELDS_DISPLAY_NAME);

		if (fieldsDisplayField == null) {
			StringBundler fieldsDisplayFieldSB = new StringBundler(fieldsMap.size() * 4 - 1);

			for (String fieldName : fields.getNames()) {
				fieldsDisplayFieldSB.append(fieldName);
				fieldsDisplayFieldSB.append(_INSTANCE_SEPARATOR);
				fieldsDisplayFieldSB.append(StringUtil.randomString());
				fieldsDisplayFieldSB.append(StringPool.COMMA);
			}

			if (fieldsDisplayFieldSB.index() > 0) {
				fieldsDisplayFieldSB.setIndex(fieldsDisplayFieldSB.index() - 1);
			}

			fieldsDisplayField = new Field(ddmStructureId, _FIELDS_DISPLAY_NAME, fieldsDisplayFieldSB.toString());

			fields.put(fieldsDisplayField);
		}

		return fields;
	}
	
	private static final String _FIELDS_DISPLAY_NAME = "_fieldsDisplay";

	private static final String _INSTANCE_SEPARATOR = "_INSTANCE_";

}
