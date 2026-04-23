package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class RoleFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(RoleFunctionalSpec)

	private static final String BASE_ROLE_NAME = 'ITTestRole'
	private static final int ROLE_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdRoleIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		(1..ROLE_COUNT).each { i ->
			try {
				def role = jsonwsGet(
					"role/get-role/company-id/${companyId}" +
					"/name/${URLEncoder.encode(BASE_ROLE_NAME + i, 'UTF-8')}") as Map

				if (role?.roleId != null) {
					jsonwsPost(
						'role/delete-role',
						['roleId': role.roleId as Long])
				}
			}
			catch (Exception e) {
				log.warn('Failed to clean up role {}: {}', BASE_ROLE_NAME + i, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Roles are created via portlet UI'() {
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

		and: 'select Roles entity type'
		page.locator('[data-testid="entity-selector-ROLES"]').click()
		page.locator('[data-testid="roles-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the role form'
		page.locator('[data-testid="roles-count-input"]').fill("${ROLE_COUNT}")
		page.locator('[data-testid="roles-base-name-input"]').fill(BASE_ROLE_NAME)

		and: 'click Run button'
		page.locator('[data-testid="roles-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="roles-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="roles-result"].alert-success').isVisible()
	}

	def 'Created roles are visible via JSONWS RoleService'() {
		when: 'look up each expected role by name'
		def roles = (1..ROLE_COUNT).collect { i ->
			jsonwsGet(
				"role/get-role/company-id/${companyId}" +
				"/name/${URLEncoder.encode(BASE_ROLE_NAME + i, 'UTF-8')}") as Map
		}

		createdRoleIds.addAll(roles.collect { it.roleId as Long })

		then: 'all created roles exist with expected names'
		roles.every { it?.roleId != null }
		roles.collect { it.name as String } ==
			(1..ROLE_COUNT).collect { "${BASE_ROLE_NAME}${it}" as String }
	}

	def 'Test roles are cleaned up via JSONWS RoleService'() {
		when:
		createdRoleIds.each { id ->
			jsonwsPost('role/delete-role', ['roleId': id])
		}

		and: 'look up each role again'
		def stillPresent = (1..ROLE_COUNT).findAll { i ->
			try {
				def r = jsonwsGet(
					"role/get-role/company-id/${companyId}" +
					"/name/${URLEncoder.encode(BASE_ROLE_NAME + i, 'UTF-8')}") as Map

				return r?.roleId != null
			}
			catch (IllegalStateException ignored) {
				return false
			}
		}

		then: 'none of the test roles remain'
		stillPresent.isEmpty()
	}

}
