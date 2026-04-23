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
class MBThreadFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(MBThreadFunctionalSpec)

	private static final String BASE_THREAD_NAME = 'ITMBThread'
	private static final String PREREQ_SECTION_TITLE = 'IT Prereq Section'
	private static final int THREAD_COUNT = 2

	@Shared
	LdfResourceClient ldf

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	long prereqCategoryId

	@Shared
	List<Long> createdThreadIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so that headless API
		// calls can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		ldf.login()

		// Discover Guest site groupId for the prereq MB category.
		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long

		// Create the prerequisite MB category (called "section" in the
		// headless-delivery API). The returned id matches the underlying
		// MBCategory primary key used by the portlet's categoryId dropdown.
		Map section = headlessPost(
			"/o/headless-delivery/v1.0/sites/${guestGroupId}" +
			'/message-board-sections',
			"{\"title\":\"${PREREQ_SECTION_TITLE}\"}")

		prereqCategoryId = section.id as Long

		log.info(
			'Created prereq MB section id={} title={}',
			prereqCategoryId, section.title)
	}

	def cleanupSpec() {
		// Deleting the MB section cascades to its threads in Liferay, so
		// per-thread deletes are best-effort and tolerated to fail.
		createdThreadIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-threads/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up MB thread {}: {}', id, e.message)
			}
		}

		if (prereqCategoryId > 0) {
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-sections" +
					"/${prereqCategoryId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up prereq MB section {}: {}',
					prereqCategoryId, e.message)
			}
		}

		ldf?.close()
		pw?.close()
	}

	def 'creates multiple MB threads under a category via UI and verifies via headless API'() {
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

		and: 'select MB Threads entity type'
		page.locator('[data-testid="entity-selector-MB_THREAD"]').click()

		and: 'wait for MB Threads form to render'
		page.locator('[data-testid="mb-thread-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill count and baseName'
		page.locator('[data-testid="mb-thread-count-input"]').fill("${THREAD_COUNT}")
		page.locator('[data-testid="mb-thread-base-name-input"]').fill(BASE_THREAD_NAME)

		and: 'select Guest site which triggers category dropdown load'
		page.locator('[data-testid="mb-thread-group-id-select"]').selectOption("${guestGroupId}")

		and: 'wait for the prereq category option to be attached to the category select'
		// Options inside a collapsed <select> are considered hidden by
		// default, so waiting with the default "visible" state fails.
		page.locator(
			"[data-testid=\"mb-thread-category-id-select\"] option[value=\"${prereqCategoryId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)

		and: 'select prereq category'
		page.locator('[data-testid="mb-thread-category-id-select"]').selectOption("${prereqCategoryId}")

		and: 'ensure body textarea has a non-empty value'
		page.locator('[data-testid="mb-thread-body-textarea"]').fill('This is a test message.')

		and: 'click Run button'
		page.locator('[data-testid="mb-thread-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="mb-thread-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="mb-thread-result"].alert-success').isVisible()

		when: 'query headless delivery API for created threads'
		// Drop ?search= — Headless Delivery goes through Elasticsearch with
		// ingestion lag after a create. Fetch with pageSize=100 and filter
		// client-side for deterministic post-condition checks.
		def response = headlessGet(
			"/o/headless-delivery/v1.0/message-board-sections" +
			"/${prereqCategoryId}/message-board-threads" +
			"?pageSize=100")

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.headline as String)?.startsWith(BASE_THREAD_NAME) ||
				(item.title as String)?.startsWith(BASE_THREAD_NAME)
		}

		createdThreadIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created MB threads are found by name prefix'
		matching.size() == THREAD_COUNT
	}

	def 'RC response items carry categoryId, groupId, messageId, subject, and threadId'() {
		given:
		Map fields = [
			count     : 1,
			baseName  : 'ITMBThreadRC',
			groupId   : guestGroupId,
			categoryId: prereqCategoryId,
			body      : 'RC contract test body.'
		]

		when: 'POST /ldf/mb-thread'
		Map response = ldf.post('/ldf/mb-thread', fields)

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
				entry.containsKey('messageId') &&
				entry.containsKey('subject') &&
				entry.containsKey('threadId')
		}

		and: 'item field values match submitted parameters'
		(response.items as List).every { (it.groupId as Long) == guestGroupId }
		(response.items as List).every {
			(it.categoryId as Long) == prereqCategoryId
		}

		cleanup:
		createdThreadIds.addAll(
			(response?.items as List)?.collect { it.threadId as Long } ?: []
		)
	}

}
