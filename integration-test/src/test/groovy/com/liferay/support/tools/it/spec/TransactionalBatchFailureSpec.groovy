package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import groovy.json.JsonOutput

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Contract: each iteration of a batch create runs in its own transaction.
 * An iteration-2 duplicate is induced by pre-creating "BASE 2" via the
 * headless API; the spec then asserts that iterations 1 and 3 survive
 * independently of iteration 2's skip.
 */
@Stepwise
class TransactionalBatchFailureSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		TransactionalBatchFailureSpec)

	private static final String BASE_ORG_NAME = 'TxBatchOrg'
	private static final int ORG_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long preExistingOrgId

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
				log.warn(
					'Failed to clean up organization {}: {}', id, e.message)
			}
		}

		if (preExistingOrgId) {
			try {
				jsonwsPost(
					'organization/delete-organization',
					['organizationId': preExistingOrgId])
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up pre-existing organization {}: {}',
					preExistingOrgId, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Pre-create the organization that will collide with iteration 2'() {
		given:
		String collidingName = "${BASE_ORG_NAME} 2"

		when: 'pre-create an organization matching iteration 2 of the batch'
		def orgResult = headlessPost(
			'/o/headless-admin-user/v1.0/organizations',
			JsonOutput.toJson([name: collidingName])
		)
		preExistingOrgId = orgResult.id as Long
		log.info(
			'Pre-created colliding organization: id={}, name={}',
			preExistingOrgId, orgResult.name)

		then:
		preExistingOrgId != null
		(orgResult.name as String) == collidingName
	}

	def 'Batch of 3 organizations runs through a mid-batch collision'() {
		given:
		Page page = pw.page

		when: 'navigate to the dummy factory portlet'
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()

		and: 'wait for the form to render'
		page.locator('[data-testid="org-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the organization batch form'
		page.locator('[data-testid="org-count-input"]').fill("${ORG_COUNT}")
		page.locator('[data-testid="org-base-name-input"]').fill(BASE_ORG_NAME)

		and: 'submit the batch'
		page.locator('[data-testid="org-submit"]').click()

		then: 'partial rollback yields an alert-danger (iteration 2 collides)'
		page.locator('[data-testid="org-result"].alert-danger').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
	}

	def 'Iteration 1 committed independently of iteration 2 rollback'() {
		when: 'query organizations via JSONWS'
		def orgs = jsonwsGet(
			"organization/get-organizations/company-id/${companyId}" +
			'/parent-organization-id/0/start/-1/end/-1') as List

		then:
		orgs != null

		when:
		def matchingItems = orgs.findAll { org ->
			(org.name as String).startsWith(BASE_ORG_NAME)
		}
		def matchingNames = matchingItems.collect { it.name as String }.sort()

		def newlyCreated = matchingItems.findAll { org ->
			(org.organizationId as Long) != preExistingOrgId
		}

		createdOrganizationIds.addAll(
			newlyCreated.collect { it.organizationId as Long }
		)

		log.info('Matching organizations after batch: {}', matchingNames)

		then: 'iteration 1 survived iteration 2 rollback'
		matchingNames.contains("${BASE_ORG_NAME} 1" as String)

		and: 'iteration 3 committed after iteration 2 rollback'
		matchingNames.contains("${BASE_ORG_NAME} 3" as String)

		and: 'the pre-existing iteration 2 row is still the original one'
		def collision = matchingItems.find { org ->
			(org.name as String) == "${BASE_ORG_NAME} 2"
		}
		collision != null
		(collision.organizationId as Long) == preExistingOrgId

		and: 'exactly two new organizations were committed by the batch'
		newlyCreated.size() == 2
	}

}
