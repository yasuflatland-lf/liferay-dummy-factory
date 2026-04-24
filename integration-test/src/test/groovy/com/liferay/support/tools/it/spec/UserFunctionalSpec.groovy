package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.LdfResourceClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class UserFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(UserFunctionalSpec)

	private static final String BASE_USER_NAME = 'ITTestUser'
	private static final int USER_COUNT = 3

	private static final String FAKER_BASE_NAME =
		"itfakeruser${System.currentTimeMillis()}"
	private static final int FAKER_USER_COUNT = 2

	@Shared
	PlaywrightLifecycle pw

	@Shared
	LdfResourceClient ldf

	@Shared
	String apiResponseBody = ''

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		ldf?.close()
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

	def 'created users have type == 1 (TYPE_REGULAR) on the fakerEnable=false branch (regression for #57)'() {
		given: 'parsed API response from the UI-driven create above'
		assert !apiResponseBody.isEmpty() : 'prior UI-create feature method did not run; @Stepwise ordering broken'
		Map response = new JsonSlurper().parseText(apiResponseBody) as Map
		List<Map> items = (response.items as List).collect { it as Map }

		expect: 'every created user has type == 1 via JSONWS get-user-by-email-address'
		items.size() == USER_COUNT

		items.every { Map item ->
			String email = item.emailAddress as String

			Map user = jsonwsGet(
				"user/get-user-by-email-address" +
				"/company-id/${companyId}" +
				"/email-address/${URLEncoder.encode(email, 'UTF-8')}"
			) as Map

			assert user != null : "user not found for email ${email}"
			assert (user.emailAddress as String)?.equalsIgnoreCase(email) :
				"JSONWS returned user emailAddress=${user.emailAddress} but we asked for ${email} — error envelope or wrong record"
			assert (user.type as Integer) == 1 :
				"expected type=1 (TYPE_REGULAR) for ${email}, got ${user.type} " +
				'(this regression hides users from Control Panel > Users and Organizations)'

			return true
		}
	}

	def 'created users have type == 1 (TYPE_REGULAR) on the fakerEnable=true branch (regression for #57)'() {
		given: 'an LDF resource client authenticated as admin'
		if (ldf == null) {
			ldf = new LdfResourceClient(liferay.baseUrl)
			ldf.login()
		}

		when: 'POST /ldf/user with fakerEnable=true and locale=en_US'
		Map response = ldf.createUser([
			count      : FAKER_USER_COUNT,
			baseName   : FAKER_BASE_NAME,
			fakerEnable: true,
			locale     : 'en_US'
		])

		then: 'response reports success'
		response.success == true
		(response.items as List).size() == FAKER_USER_COUNT

		and: 'all returned screen names match Liferay-legal characters (deterministic lock for sanitized faker output)'
		(response.items as List).every {
			(it.screenName as String) ==~ /^[a-z0-9._-]+$/
		}

		and: 'every faker-generated user has type == 1 via JSONWS get-user-by-email-address'
		(response.items as List).every { item ->
			String email = (item as Map).emailAddress as String

			Map user = jsonwsGet(
				"user/get-user-by-email-address" +
				"/company-id/${companyId}" +
				"/email-address/${URLEncoder.encode(email, 'UTF-8')}"
			) as Map

			assert user != null : "user not found for email ${email}"
			assert (user.emailAddress as String)?.equalsIgnoreCase(email) :
				"JSONWS returned user emailAddress=${user.emailAddress} but we asked for ${email} — error envelope or wrong record"
			assert (user.type as Integer) == 1 :
				"expected type=1 (TYPE_REGULAR) for ${email}, got ${user.type} " +
				'(this regression hides users from Control Panel > Users and Organizations)'

			return true
		}
	}

}
