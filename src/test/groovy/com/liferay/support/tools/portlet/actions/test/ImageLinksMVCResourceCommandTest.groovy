package com.liferay.support.tools.portlet.actions.test

import com.liferay.support.tools.portlet.actions.ImageLinksMVCResourceCommand
import io.reactivex.Flowable
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.IntStream

class ImageLinksMVCResourceCommandTest extends Specification {
    @Unroll
    def "Run Smoke Test amount<#amount> result<#resultAmount>"() {
        when:
        ImageLinksMVCResourceCommand ilcommand = Spy(ImageLinksMVCResourceCommand) {
            getImagesFromURL(*_) >> Flowable.just(["a", "b"])
        }
        List<String> urls = new ArrayList<String>()
        IntStream.range(0, amount).forEach({ i ->
            urls.add("url" + i)
        })

        List<String> result = ilcommand.runEx(0, 0, 0, urls, 0).blockingSingle()

        then:
        result.size() == resultAmount

        where:
        amount | resultAmount
        2      | 4
        4      | 8
        8      | 16
    }

    @Unroll("ImageCrawlController test")
    def "ImageCrawlController test"() {
        when:
        ImageLinksMVCResourceCommand ilcommand = Spy(ImageLinksMVCResourceCommand)
        List<String> results = ilcommand.exec(10, 2, 20, targeturl, 10);

        then:
        0 != results.size()

        where:
        targeturl            | _
        "https://imgur.com/" | _
    }
}
