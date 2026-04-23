package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class SiteFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(SiteFunctionalSpec)

	private static final String BASE_SITE_NAME = 'ITTestSite'
	private static final int SITE_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdSiteIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdSiteIds.each { id ->
			try {
				jsonwsPost(
					'group/delete-group',
					['groupId': id])
			}
			catch (Exception e) {
				log.warn('Failed to clean up site {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Sites are created via portlet UI'() {
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

		and: 'select Sites entity type'
		page.locator('[data-testid="entity-selector-SITES"]').click()

		and: 'wait for Sites form to render'
		page.locator('[data-testid="sites-submit"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="sites-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the site form'
		page.locator('[data-testid="sites-count-input"]').fill("${SITE_COUNT}")
		page.locator('[data-testid="sites-base-name-input"]').fill(BASE_SITE_NAME)

		and: 'click Run button'
		page.locator('[data-testid="sites-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="sites-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="sites-result"].alert-success').isVisible()
	}

	def 'Created sites are visible via JSONWS GroupService'() {
		when:
		def groups = jsonwsGet(
			"group/get-groups/company-id/${companyId}" +
			'/parent-group-id/0/site/true/start/-1/end/-1') as List

		then:
		groups != null

		when:
		def matchingSites = groups.findAll { group ->
			def name = (group.nameCurrentValue ?: group.name) as String
			name?.startsWith(BASE_SITE_NAME)
		}

		createdSiteIds.addAll(
			matchingSites.collect { it.groupId as Long }
		)

		then: 'all created sites are found by name prefix'
		matchingSites.size() == SITE_COUNT
	}

	def 'Test sites are cleaned up via JSONWS GroupService'() {
		when: 'delete each created site'
		createdSiteIds.each { id ->
			jsonwsPost(
				'group/delete-group',
				['groupId': id])
		}

		and: 'list groups again'
		def groups = jsonwsGet(
			"group/get-groups/company-id/${companyId}" +
			'/parent-group-id/0/site/true/start/-1/end/-1') as List

		then: 'none of the test sites remain'
		!groups.any { group ->
			def name = (group.nameCurrentValue ?: group.name) as String
			name?.startsWith(BASE_SITE_NAME)
		}
	}

}
