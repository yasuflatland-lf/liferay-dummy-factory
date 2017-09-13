package com.liferay.support.tools.portlet.actions.test

import com.liferay.support.tools.portlet.actions.UserDataService

import spock.lang.Specification
import spock.lang.Unroll

class UserDataServiceTest extends Specification {
	@Unroll("createFaker test scucess Locale")
	def "createFaker test scucess"() {
		when:
		def uds = new UserDataService();

		then:
		[
			"bg",
			"ca",
			"ca-CAT",
			"da-DK",
			"de",
			"de-AT",
			"de-CH",
			"en",
			"en-AU",
			"en-au-ocker",
			"en-BORK",
			"en-CA",
			"en-GB",
			"en-IND",
			"en-NEP",
			"en-NG",
			"en-NZ",
			"en-PAK",
			"en-SG",
			"en-UG",
			"en-US",
			"en-ZA",
			"es",
			"es-MX",
			"fa",
			"fi-FI",
			"fr",
			"he",
			"in-ID",
			"it",
			"ja",
			"ko",
			"nb-NO",
			"nl",
			"pl",
			"pt",
			"pt-BR",
			"ru",
			"sk",
			"sv",
			"sv-SE",
			"tr",
			"uk",
			"vi",
			"zh-CN",
			"zh-TW"
		].each{ lang ->
			assert null != uds.createFaker(lang);
		}
	}

	@Unroll("createFaker test unavailable Locale <#lang>")
	def "createFaker test unavailable locales"() {
		when:
		def uds = new UserDataService();

		then:
		["fl", "th"].each{ lang ->
			assert null != uds.createFaker(lang);
		}
	}

	@Unroll("getFakerAvailableLocales test available Locale")
	def "getFakerAvailableLocales test available locales"() {
		when:
		def uds = new UserDataService();
		Set<Locale> defaultLocale = new HashSet<>();
		["es", "ja", "nl", "hu", "pt", "de", "iw", "fi", "ca", "fr"].each {
			localeStr ->
			defaultLocale.add(new Locale(localeStr));
		}
		List<Locale> ret = uds.getFakerAvailableLocales(defaultLocale);

		then:
		["de", "es", "pt", "fr", "ja", "nl", "ca"].each {
			localeStr ->
			assert ret.contains(new Locale(localeStr))
		}
	}
}
