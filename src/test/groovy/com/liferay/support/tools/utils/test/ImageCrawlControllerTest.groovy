package com.liferay.support.tools.utils.test

import com.liferay.support.tools.utils.ImageCrawlController
import spock.lang.Specification
import spock.lang.Unroll

class ImageCrawlControllerTest extends Specification {
    @Unroll("ImageCrawlController test")
    def "ImageCrawlController test"() {
        when:
        def icc = new ImageCrawlController();
        icc.exec(10, 2, 20, targeturl, 10);
        List<String> results = icc.getURL();

        then:
        0 != results.size()

        where:
        targeturl            | _
        "https://imgur.com/" | _
    }
}
