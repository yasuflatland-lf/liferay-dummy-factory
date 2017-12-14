package com.liferay.support.tools.portlet.actions.test

import com.liferay.support.tools.user.UserDataService
import spock.lang.Specification
import spock.lang.Unroll

class UserDataServiceTest extends Specification {

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
