package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class PortletRenderSpec extends BaseLiferaySpec {

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<String> jsErrors = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Portlet renders without JavaScript errors'() {
		given:
		Page page = pw.page

		page.onConsoleMessage(msg -> {
			if (msg.type() == 'error') {
				jsErrors.add(msg.text())
			}
		})

		page.onPageError(error -> {
			jsErrors.add(error)
		})

		when:
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()

		then: 'React component renders'
		page.locator('[data-testid="org-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="org-count-input"]').isVisible()

		and: 'no critical JavaScript errors in console'
		jsErrors.findAll {
			it.contains('ERR_ABORTED') ||
			it.contains('not supported') ||
			it.contains('Failed to fetch dynamically imported module') ||
			it.contains('404')
		}.empty
	}

	def 'ESM bundle loads from __liferay__ path'() {
		when:
		def responseCode = httpGet(
			"${liferay.baseUrl}/o/liferay-dummy-factory/__liferay__/index.js"
		)

		then:
		responseCode == 200
	}

	def 'React external resolves from Liferay runtime'() {
		when:
		def responseCode = httpGet(
			"${liferay.baseUrl}/o/frontend-js-react-web/__liferay__/exports/react.js"
		)

		then:
		responseCode == 200
	}

}
