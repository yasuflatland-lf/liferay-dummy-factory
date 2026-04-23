package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.Locale;

import net.datafaker.Faker;

import org.osgi.service.component.annotations.Component;

/**
 * Common Library
 *
 * @author Yasuyuki Takeo
 */
@Component(service = CommonUtil.class)
public class CommonUtil {

	/**
	 * Create Faker
	 *
	 * @param locale Language to create Faker object based on.
	 * @return Faker object.
	 */
	public Faker createFaker(String locale) {
		Faker faker = new Faker(new Locale(Locale.ENGLISH.toLanguageTag()));

		try {
			Faker fakerTmp = new Faker(new Locale(locale));
			faker = fakerTmp;
		}
		catch (Exception e) {
			_log.error(e.getMessage(), e);
		}

		return faker;
	}

	private static final Log _log = LogFactoryUtil.getLog(CommonUtil.class);

}
