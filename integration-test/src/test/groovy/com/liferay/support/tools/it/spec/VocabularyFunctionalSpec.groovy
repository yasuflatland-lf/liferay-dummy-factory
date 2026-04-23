package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.LdfResourceClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitForSelectorState

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class VocabularyFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(VocabularyFunctionalSpec)

	private static final String BASE_VOCAB_NAME = 'ITVocab'
	private static final int VOCAB_COUNT = 3

	@Shared
	LdfResourceClient ldf

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	List<Long> createdVocabularyIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so that headless API
		// calls can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		ldf.login()

		// Discover Guest site groupId for vocabulary creation.
		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long
	}

	def cleanupSpec() {
		createdVocabularyIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up vocabulary {}: {}', id, e.message)
			}
		}

		ldf?.close()
		pw?.close()
	}

	def 'Vocabularies are created via portlet UI'() {
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

		and: 'select Vocabularies entity type'
		page.locator('[data-testid="entity-selector-VOCABULARY"]').click()

		and: 'wait for Vocabularies form to render'
		page.locator('[data-testid="vocabulary-submit"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="vocabulary-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the vocabularies form'
		page.locator('[data-testid="vocabulary-count-input"]').fill("${VOCAB_COUNT}")
		page.locator('[data-testid="vocabulary-base-name-input"]').fill(BASE_VOCAB_NAME)
		page.locator(
			"[data-testid=\"vocabulary-group-id-select\"] option[value=\"${guestGroupId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)
		page.locator('[data-testid="vocabulary-group-id-select"]').selectOption("${guestGroupId}")

		and: 'click Run button'
		page.locator('[data-testid="vocabulary-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="vocabulary-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="vocabulary-result"].alert-success').isVisible()
	}

	def 'Created vocabularies are visible via headless taxonomy API'() {
		when:
		// Drop ?search= — Headless Delivery goes through Elasticsearch which has
		// ingestion lag after a create. Fetch with pageSize=100 and filter
		// client-side for deterministic post-condition checks.
		def response = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/sites/${guestGroupId}" +
			"/taxonomy-vocabularies?pageSize=100")

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.name as String)?.startsWith(BASE_VOCAB_NAME)
		}

		createdVocabularyIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created vocabularies are found by name prefix'
		matching.size() == VOCAB_COUNT
	}

	def 'RC response items carry vocabularyId, groupId, and name'() {
		given:
		Map fields = [
			count   : 1,
			baseName: 'ITVocabRC',
			groupId : guestGroupId
		]

		when: 'POST /ldf/vocabulary'
		Map response = ldf.post('/ldf/vocabulary', fields)

		then: 'outer batch contract fields are present'
		response.containsKey('success')
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')

		and: 'batch was successful'
		response.success == true
		(response.count as Integer) == 1
		(response.requested as Integer) == 1
		(response.skipped as Integer) == 0

		and: 'each item carries all parity fields'
		(response.items as List).every { item ->
			Map entry = item as Map
			entry.containsKey('vocabularyId') &&
				entry.containsKey('groupId') &&
				entry.containsKey('name')
		}

		and: 'item groupId matches submitted parameter'
		(response.items as List).every { (it.groupId as Long) == guestGroupId }

		cleanup:
		createdVocabularyIds.addAll(
			(response?.items as List)?.collect { it.vocabularyId as Long } ?: []
		)
	}

}
