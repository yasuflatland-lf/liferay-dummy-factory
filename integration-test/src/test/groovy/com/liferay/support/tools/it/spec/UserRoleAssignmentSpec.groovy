package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Response

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class UserRoleAssignmentSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(UserRoleAssignmentSpec)

	private static final String RUN_SUFFIX = String.valueOf(System.currentTimeMillis())
	private static final String BASE_USER_NAME = "ITRoleUser${RUN_SUFFIX}"
	private static final String TEST_ORG_NAME = "ITRoleTestOrg${RUN_SUFFIX}"

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long testOrgId

	@Shared
	Long createdUserId

	@Shared
	String apiResponseBody = ''

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		if (createdUserId) {
			try {
				jsonwsPost(
					'user/delete-user',
					['userId': createdUserId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up user {}: {}', createdUserId, e.message)
			}
		}

		if (testOrgId) {
			try {
				jsonwsPost(
					'organization/delete-organization',
					['organizationId': testOrgId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up organization {}: {}', testOrgId, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Create test organization for role assignment'() {
		when: 'create fresh organization via headless REST (DB-backed create)'
		def orgResult = headlessPost(
			'/o/headless-admin-user/v1.0/organizations',
			JsonOutput.toJson([name: TEST_ORG_NAME])
		)
		testOrgId = orgResult.id as Long
		log.info('Created test organization: id={}, name={}', testOrgId, orgResult.name)

		then:
		testOrgId != null
	}

	def 'User is created with organization assignment via portlet UI'() {
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
		page.locator('[data-testid="users-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in user creation fields'
		page.locator('[data-testid="users-count-input"]').fill('1')
		page.locator('[data-testid="users-base-name-input"]').fill(BASE_USER_NAME)

		and: 'wait for advanced field to render (section is open-by-default after EntityForm refactor)'
		page.locator('[data-testid="users-organization-ids-select"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'select the test organization in the multiselect'
		page.locator("[data-testid=\"users-organization-ids-select\"] option[value=\"${testOrgId}\"]").click()

		and: 'click Run button and capture the /ldf/user resource response'
		Response response = page.waitForResponse(
			{ Response r -> r.url().contains('p_p_resource_id=%2Fldf%2Fuser') },
			{ -> page.locator('[data-testid="users-submit"]').click() })
		apiResponseBody = response.text()

		then: 'success alert appears'
		page.locator('[data-testid="users-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="users-result"].alert-success').isVisible()
	}

	def 'Created user is visible and belongs to test organization via JSONWS'() {
		given:
		String expectedScreenName = BASE_USER_NAME.toLowerCase() + '1'

		expect: 'portlet API response captured the created user id'
		apiResponseBody.contains('"success":true')

		when: 'extract userId from portlet API response'
		def apiJson = new JsonSlurper().parseText(apiResponseBody) as Map
		def firstUser = (apiJson.items as List)?.first() as Map
		createdUserId = Long.parseLong(firstUser.userId as String)

		and: 'fetch the user by id via JSONWS (DB-backed)'
		def user = jsonwsGet(
			"user/get-user-by-id/user-id/${createdUserId}") as Map

		then: 'screen name matches'
		(user.screenName as String) == expectedScreenName

		when: 'fetch user organizations via JSONWS'
		def userOrgs = jsonwsGet(
			"organization/get-user-organizations/user-id/${createdUserId}") as List

		then: 'organization membership reflects testOrgId'
		userOrgs.any { (it.organizationId as Long) == testOrgId }
	}

	def 'Test organization lists the created user as a member via JSONWS'() {
		given:
		String expectedScreenName = BASE_USER_NAME.toLowerCase() + '1'

		when: 'query organization users via JSONWS (DB-backed, no index lag)'
		def users = jsonwsGet(
			"user/get-organization-users/organization-id/${testOrgId}"
		) as List

		then: 'the created user appears in the organization member list'
		users?.any { (it.screenName as String) == expectedScreenName }
	}

}
