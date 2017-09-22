package com.liferay.support.tools.utils.test

import com.liferay.support.tools.utils.ImageCrawlController

import spock.lang.Specification
import spock.lang.Unroll

class ImageCrawlControllerTest extends Specification {
	@Unroll("ImageCrawlController test")
	def "ImageCrawlController test"() {
		when:
		List<String> targets = new ArrayList<>();
		
		targeturls.each{ s ->
			targets.add(s)
		}
		String[] urlarray = targets.toArray(new String[targets.size()]);
		ImageCrawlController.run(15, 20, urlarray);
		List<String> results = ImageCrawlController.getURL();
		
		then:
		0 == results.size()
		
		where:
		targeturls | _
		["https://imgur.com/","https://www.shutterstock.com/photos"] | _
	}
}
