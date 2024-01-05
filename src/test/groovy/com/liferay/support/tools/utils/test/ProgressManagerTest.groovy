package com.liferay.support.tools.utils.test

import com.liferay.support.tools.utils.ProgressManager
import spock.lang.Specification
import spock.lang.Unroll

class ProgressManagerTest extends Specification {
	@Unroll("ImageCrawlController test index<#index> Number of Total <#numberOfTotal> should be <#percent> percent")
	def "ProgressManager Test"() {
		when:
		ProgressManager pm = Spy(ProgressManager.class)
		def result = pm.percentageCalcluation(index, numberOfTotal)
		
		then:
		percent == result
		
		where:
		index | numberOfTotal | percent
		1     | 0             | 0
		0     | 5             | 0
		1	  | 3			  | 33
		2	  | 100			  | 2
		2     | 200	          | 1
		10	  | 3			  | 100
	}
}