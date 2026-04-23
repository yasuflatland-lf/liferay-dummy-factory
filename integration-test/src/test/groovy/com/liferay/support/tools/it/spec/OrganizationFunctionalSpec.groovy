package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class OrganizationFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(OrganizationFunctionalSpec)

	private static final String BASE_ORG_NAME = 'IT Test Org'
	private static final int ORG_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdOrganizationIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdOrganizationIds.each { id ->
			try {
				jsonwsPost(
					'organization/delete-organization',
					['organizationId': id])
			}
			catch (Exception e) {
				log.warn('Failed to clean up organization {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Organizations are created via portlet UI'() {
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

		and: 'select Organization entity type'
		page.locator('[data-testid="entity-selector-ORG"]').click()

		and: 'wait for the form to render'
		page.locator('[data-testid="org-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the organization form'
		page.locator('[data-testid="org-count-input"]').fill("${ORG_COUNT}")
		page.locator('[data-testid="org-base-name-input"]').fill(BASE_ORG_NAME)

		and: 'click Run button'
		page.locator('[data-testid="org-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="org-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="org-result"].alert-success').isVisible()
	}

	def 'Created organizations are visible via JSONWS OrganizationService'() {
		when:
		def orgs = jsonwsGet(
			"organization/get-organizations/company-id/${companyId}" +
			'/parent-organization-id/0/start/-1/end/-1') as List

		then:
		orgs != null

		when:
		def matchingItems = orgs.findAll { org ->
			(org.name as String).startsWith(BASE_ORG_NAME)
		}

		createdOrganizationIds.addAll(
			matchingItems.collect { it.organizationId as Long }
		)

		then: 'all created organizations exist with expected names'
		matchingItems.size() == ORG_COUNT
		matchingItems.collect { it.name as String }.sort() ==
			(1..ORG_COUNT).collect { "${BASE_ORG_NAME} ${it}" }
	}

	def 'Re-creating same organizations is handled gracefully'() {
		given:
		Page page = pw.page

		when: 'navigate to portlet and submit same names again'
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()
		page.locator('[data-testid="entity-selector-ORG"]').click()
		page.locator('[data-testid="org-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="org-count-input"]').fill("${ORG_COUNT}")
		page.locator('[data-testid="org-base-name-input"]').fill(BASE_ORG_NAME)
		page.locator('[data-testid="org-submit"]').click()

		then: 'alert appears (re-creating duplicates yields alert-danger)'
		page.locator('[data-testid="org-result"].alert-danger').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)

		and: 'organization count has not increased'
		def orgs = jsonwsGet(
			"organization/get-organizations/company-id/${companyId}" +
			'/parent-organization-id/0/start/-1/end/-1') as List
		def matchingItems = orgs.findAll { org ->
			(org.name as String).startsWith(BASE_ORG_NAME)
		}
		matchingItems.size() == ORG_COUNT
	}

	def 'Test organizations are cleaned up via JSONWS OrganizationService'() {
		when:
		createdOrganizationIds.each { id ->
			jsonwsPost(
				'organization/delete-organization',
				['organizationId': id])
		}

		and: 'list organizations again'
		def orgs = jsonwsGet(
			"organization/get-organizations/company-id/${companyId}" +
			'/parent-organization-id/0/start/-1/end/-1') as List

		then: 'none of the test organizations remain'
		!orgs.any { org ->
			(org.name as String).startsWith(BASE_ORG_NAME)
		}
	}

}
