package com.liferay.support.tools.portlet.actions.test

import com.liferay.support.tools.portlet.actions.DummyFactoryConfigurationAction
import spock.lang.Specification
import spock.lang.Unroll

class DummyFactoryConfigurationActionTest extends Specification {
    @Unroll("split test <#test_str> <#ret_size>")
    def "split test"() {
        when:
        String[] resultstr = test_str.split(",");
        List<String> lstresult = new ArrayList<>(Arrays.asList(resultstr));

        then:
        lstresult.size() >= ret_size;

        where:
        test_str                                                 | ret_size
        "https://imgur.com/,https://www.shutterstock.com/photos" | 2
        "https://imgur.com/"                                     | 1
        ""                                                       | 0
    }

    @Unroll("validate test <#test_str> <#comp_ret>")
    def "validate test"() {
        when:
        DummyFactoryConfigurationAction dca = new DummyFactoryConfigurationAction();
        List<String> errors = new ArrayList<>();
        Boolean ret = dca.validate(test_str, errors);

        then:
        ret == comp_ret;

        where:
        test_str                                                 | comp_ret
        "https://imgur.com/,https://www.shutterstock.com/photos" | true
        "https://imgur.com/"                                     | true
        ""                                                       | true
    }
}
