package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitForSelectorState

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class DocumentFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(DocumentFunctionalSpec)

	private static final String BASE_DOC_NAME = 'ITTestDoc'
	private static final int DOC_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	List<Long> createdFileEntryIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdFileEntryIds.each { id ->
			try {
				jsonwsPost(
					'dlapp/delete-file-entry',
					['fileEntryId': id])
			}
			catch (Exception e) {
				log.warn('Failed to clean up file entry {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Discover Guest site groupId'() {
		when:
		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		then:
		group != null
		group.groupId != null

		when:
		guestGroupId = group.groupId as Long

		then:
		guestGroupId > 0
	}

	def 'Documents are created via portlet UI'() {
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

		and: 'select Documents entity type'
		page.locator('[data-testid="entity-selector-DOC"]').click()

		and: 'wait for Documents form to render'
		page.locator('[data-testid="doc-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the documents form'
		page.locator('[data-testid="doc-count-input"]').fill("${DOC_COUNT}")
		page.locator('[data-testid="doc-base-name-input"]').fill(BASE_DOC_NAME)
		page.locator(
			"[data-testid=\"doc-group-id-select\"] option[value=\"${guestGroupId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)
		page.locator('[data-testid="doc-group-id-select"]').selectOption("${guestGroupId}")

		and: 'click Run button'
		page.locator('[data-testid="doc-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="doc-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="doc-result"].alert-success').isVisible()
	}

	def 'Created documents are visible via JSONWS DLAppService'() {
		when:
		def entries = jsonwsGet(
			"dlapp/get-file-entries/repository-id/${guestGroupId}" +
			'/folder-id/0') as List

		then:
		entries != null

		when:
		def matchingEntries = entries.findAll { entry ->
			(entry.title as String)?.startsWith(BASE_DOC_NAME)
		}

		createdFileEntryIds.addAll(
			matchingEntries.collect { it.fileEntryId as Long }
		)

		then: 'all created documents are found by title prefix'
		matchingEntries.size() == DOC_COUNT
	}

}
