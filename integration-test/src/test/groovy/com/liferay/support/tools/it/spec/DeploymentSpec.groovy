package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.GogoShellClient

import java.net.ConnectException
import java.net.SocketTimeoutException

import spock.lang.Stepwise

@Stepwise
class DeploymentSpec extends BaseLiferaySpec {

	def 'Liferay container starts and is accessible'() {
		expect:
		liferay.running

		when:
		int responseCode = -1
		int maxRetries = 3

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				responseCode = httpGet("${liferay.baseUrl}/c/portal/login")
				break
			}
			catch (SocketTimeoutException | ConnectException e) {
				if (attempt < maxRetries) {
					Thread.sleep(10_000)
				}
				else {
					throw e
				}
			}
		}

		then:
		responseCode == 200
	}

	def 'Liferay Dummy Factory JAR deploys and bundle becomes ACTIVE'() {
		when:
		ensureBundleActive()

		and:
		String output = ''

		new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
			output = gogo.execute('lb')
		}
		def matchingLine = output.readLines().find { it.contains('Liferay Dummy Factory') }

		then:
		matchingLine != null
		matchingLine.contains('Active')
	}

	def 'Portlet web resources are accessible after deployment'() {
		given:
		ensureBundleActive()

		when:
		def responseCode = httpGet(
			"${liferay.baseUrl}/o/liferay-dummy-factory/__liferay__/index.js"
		)

		then:
		responseCode == 200
	}

}
