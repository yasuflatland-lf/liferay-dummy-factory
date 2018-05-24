package com.liferay.support.tools.utils.test

import com.liferay.support.tools.utils.CommonUtil
import spock.lang.Specification
import spock.lang.Unroll

class CommonUtilTest extends Specification {
    @Unroll("createFaker test scucess Locale")
    def "createFaker test scucess"() {
        when:
        def uds = new CommonUtil();

        then:
        [
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
        ].each { lang ->
            assert null != uds.createFaker(lang);
        }
    }

    @Unroll("createFaker test unavailable Locale <#lang>")
    def "createFaker test unavailable locales"() {
        when:
        def uds = new CommonUtil();

        then:
        ["bg", "fl", "th"].each { lang ->
            assert null != uds.createFaker(lang);
        }
    }
	
	@Unroll("convertToStringArray test") 
	def "convertToStringArray test" () {
		when:
		def emptyStrArray = new String[0];
		
		then:
		def ret = CommonUtil.convertToStringArray(input_string);
		ret == result
		size_ret == ret.size();
		
		where:
		result                  								  | input_string    									  | size_ret
		["http://trackbacktest.com","https://www.trackback2.com"] | "http://trackbacktest.com,https://www.trackback2.com" | 2
		["aaa","bbb"]											  | "aaa,bbb"											  | 2
		[]                                                        | ""           										  | 0
	}
}
