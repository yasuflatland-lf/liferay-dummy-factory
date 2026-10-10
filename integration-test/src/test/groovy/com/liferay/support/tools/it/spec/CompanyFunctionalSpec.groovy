package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class CompanyFunctionalSpec extends BaseLiferaySpec {

	private static final int COMPANY_COUNT = 1
	private static final String COMPANY_WEB_ID = 'ittestco'
	private static final String COMPANY_VIRTUAL_HOSTNAME = 'ittestco.example.com'
	private static final String COMPANY_MX = 'ittestco.example.com'

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long createdCompanyId

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin credentials so JSON-WS calls can authenticate.
		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		// CompanyService is excluded from JSON-WS by Liferay's default
		// json.service.invalid.class.names, so there is no remote delete path.
		// The workspace-managed Docker container is disposable per run, so the
		// created company is discarded with the container at the end of the run.
		pw?.close()
	}

	def 'Company is created via portlet UI'() {
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

		and: 'select Company entity type'
		page.locator('[data-testid="entity-selector-COMPANY"]').click()

		and: 'wait for Company form to render'
		page.locator('[data-testid="company-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the company form'
		page.locator('[data-testid="company-count-input"]').fill("${COMPANY_COUNT}")
		page.locator('[data-testid="company-web-id-input"]').fill(COMPANY_WEB_ID)
		page.locator('[data-testid="company-virtual-hostname-input"]').fill(COMPANY_VIRTUAL_HOSTNAME)
		page.locator('[data-testid="company-mx-input"]').fill(COMPANY_MX)

		and: 'click Run button'
		page.locator('[data-testid="company-submit"]').click()

		then: 'success alert appears'
		// addCompany runs the site initializers synchronously (about 10 s
		// locally), so leave headroom for slower CI runners.
		page.locator('[data-testid="company-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(90_000)
		)
		page.locator('[data-testid="company-result"].alert-success').isVisible()
	}

}
