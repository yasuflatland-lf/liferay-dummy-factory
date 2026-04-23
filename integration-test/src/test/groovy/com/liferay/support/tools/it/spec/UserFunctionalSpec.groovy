package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class UserFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(UserFunctionalSpec)

	private static final String BASE_USER_NAME = 'ITTestUser'
	private static final int USER_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	String apiResponseBody = ''

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

	def 'Users are created via portlet UI'() {
		given:
		Page page = pw.page

		when: 'navigate to portlet'
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()

		and: 'select Users entity type'
		page.locator('[data-testid="entity-selector-USERS"]').click()

		and: 'wait for Users form to render'
		page.locator('[data-testid="users-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the user form'
		page.locator('[data-testid="users-count-input"]').fill("${USER_COUNT}")
		page.locator('[data-testid="users-base-name-input"]').fill(BASE_USER_NAME)

		and: 'capture API response and click Run'
		page.onResponse(response -> {
			try {
				String body = response.text()

				if (body?.contains('"items"')) {
					apiResponseBody = body
				}
			}
			catch (ignored) {}
		})

		page.locator('[data-testid="users-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="users-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="users-result"].alert-success').isVisible()
	}

	def 'API response confirms users were created'() {
		expect:
		log.info('API response: {}', apiResponseBody)
		apiResponseBody.contains('"success":true')
		apiResponseBody.contains("\"count\":${USER_COUNT}")

		and: 'response contains expected screen names under items key'
		String prefix = BASE_USER_NAME.toLowerCase()

		(1..USER_COUNT).every { i ->
			apiResponseBody.contains("\"screenName\":\"${prefix}${i}\"")
		}

		and: 'response uses items key (not legacy users key)'
		apiResponseBody.contains('"items"')
		!apiResponseBody.contains('"users"')
	}

}
