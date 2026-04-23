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
class CategoryFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(CategoryFunctionalSpec)

	private static final String BASE_CATEGORY_NAME = 'ITCategory'
	private static final String PREREQ_VOCAB_NAME = 'IT Prereq Vocab'
	private static final int CATEGORY_COUNT = 3

	@Shared
	LdfResourceClient ldf

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	long prereqVocabularyId

	@Shared
	List<Long> createdCategoryIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so that headless API
		// calls can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		ldf.login()

		// Discover Guest site groupId for the prereq vocabulary.
		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long

		// Create the prerequisite vocabulary via Headless Admin Taxonomy API.
		Map vocab = headlessPost(
			"/o/headless-admin-taxonomy/v1.0/sites/${guestGroupId}" +
			'/taxonomy-vocabularies',
			"{\"name\":\"${PREREQ_VOCAB_NAME}\"}")

		prereqVocabularyId = vocab.id as Long

		log.info(
			'Created prereq vocabulary id={} name={}',
			prereqVocabularyId, vocab.name)
	}

	def cleanupSpec() {
		createdCategoryIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-categories/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up category {}: {}', id, e.message)
			}
		}

		if (prereqVocabularyId > 0) {
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" +
					"/${prereqVocabularyId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up prereq vocabulary {}: {}',
					prereqVocabularyId, e.message)
			}
		}

		ldf?.close()
		pw?.close()
	}

	def 'creates multiple categories under vocabulary via UI and verifies via headless API'() {
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

		and: 'select Categories entity type'
		page.locator('[data-testid="entity-selector-CATEGORY"]').click()

		and: 'wait for Categories form to render'
		page.locator('[data-testid="category-submit"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="category-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill count and baseName'
		page.locator('[data-testid="category-count-input"]').fill("${CATEGORY_COUNT}")
		page.locator('[data-testid="category-base-name-input"]').fill(BASE_CATEGORY_NAME)

		and: 'select Guest site which triggers vocabulary dropdown load'
		page.locator('[data-testid="category-group-id-select"]').selectOption("${guestGroupId}")

		and: 'wait for vocabulary dropdown to populate with prereq vocab'
		page.locator(
			"[data-testid=\"category-vocabulary-id-select\"] option[value=\"${prereqVocabularyId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)

		and: 'select prereq vocabulary'
		page.locator('[data-testid="category-vocabulary-id-select"]').selectOption("${prereqVocabularyId}")

		and: 'click Run button'
		page.locator('[data-testid="category-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="category-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="category-result"].alert-success').isVisible()

		when: 'query headless taxonomy API for created categories'
		// Drop ?search= — Headless Delivery goes through Elasticsearch with
		// ingestion lag after a create. Fetch with pageSize=100 and filter
		// client-side for deterministic post-condition checks.
		def response = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" +
			"/${prereqVocabularyId}/taxonomy-categories" +
			"?pageSize=100")

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.name as String)?.startsWith(BASE_CATEGORY_NAME)
		}

		createdCategoryIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created categories are found by name prefix'
		matching.size() == CATEGORY_COUNT
	}

	def 'RC response items carry categoryId, groupId, vocabularyId, and name'() {
		given:
		Map fields = [
			count       : 1,
			baseName    : 'ITCatRC',
			groupId     : guestGroupId,
			vocabularyId: prereqVocabularyId
		]

		when: 'POST /ldf/category'
		Map response = ldf.post('/ldf/category', fields)

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
			entry.containsKey('categoryId') &&
				entry.containsKey('groupId') &&
				entry.containsKey('vocabularyId') &&
				entry.containsKey('name')
		}

		and: 'item field values match submitted parameters'
		(response.items as List).every { (it.groupId as Long) == guestGroupId }
		(response.items as List).every {
			(it.vocabularyId as Long) == prereqVocabularyId
		}

		cleanup:
		createdCategoryIds.addAll(
			(response?.items as List)?.collect { it.categoryId as Long } ?: []
		)
	}

}
