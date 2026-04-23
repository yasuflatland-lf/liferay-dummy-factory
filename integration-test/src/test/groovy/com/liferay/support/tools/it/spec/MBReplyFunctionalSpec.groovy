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
class MBReplyFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(MBReplyFunctionalSpec)

	private static final String PREREQ_SECTION_TITLE = 'IT Prereq Section For Replies'
	private static final String PREREQ_THREAD_HEADLINE = 'IT Prereq Thread'
	private static final int REPLY_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	long prereqSectionId

	@Shared
	long prereqThreadId

	@Shared
	List<Long> createdReplyIds = []

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so headless API calls
		// can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		// Discover Guest site groupId for prereq section creation.
		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long

		// Create the prerequisite MB section via Headless Delivery API.
		Map section = headlessPost(
			"/o/headless-delivery/v1.0/sites/${guestGroupId}" +
			'/message-board-sections',
			"{\"title\":\"${PREREQ_SECTION_TITLE}\"}")

		prereqSectionId = section.id as Long

		log.info(
			'Created prereq MB section id={} title={}',
			prereqSectionId, section.title)

		// Create the prerequisite MB thread under the section.
		Map thread = headlessPost(
			"/o/headless-delivery/v1.0/message-board-sections" +
			"/${prereqSectionId}/message-board-threads",
			"{\"headline\":\"${PREREQ_THREAD_HEADLINE}\"," +
			"\"articleBody\":\"Prereq thread body.\"}")

		prereqThreadId = thread.id as Long

		log.info(
			'Created prereq MB thread id={} headline={}',
			prereqThreadId, thread.headline)
	}

	def cleanupSpec() {
		// Deleting the thread cascades replies; the section must be deleted
		// after the thread.
		if (prereqThreadId > 0) {
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-threads" +
					"/${prereqThreadId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up prereq thread {}: {}',
					prereqThreadId, e.message)
			}
		}

		if (prereqSectionId > 0) {
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-sections" +
					"/${prereqSectionId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up prereq section {}: {}',
					prereqSectionId, e.message)
			}
		}

		pw?.close()
	}

	def 'creates multiple MB replies under thread via UI and verifies via headless API'() {
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

		and: 'select MB Replies entity type'
		page.locator('[data-testid="entity-selector-MB_REPLY"]').click()

		and: 'wait for MB Replies form to render'
		page.locator('[data-testid="mb-reply-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill count'
		page.locator('[data-testid="mb-reply-count-input"]').fill("${REPLY_COUNT}")

		and: 'fill base name'
		page.locator('[data-testid="mb-reply-base-name-input"]').fill('test-reply')

		and: 'wait for thread dropdown to populate with prereq thread'
		page.locator(
			"[data-testid=\"mb-reply-thread-id-select\"] option[value=\"${prereqThreadId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)

		and: 'select prereq thread'
		page.locator('[data-testid="mb-reply-thread-id-select"]').selectOption("${prereqThreadId}")

		and: 'click Run button'
		page.locator('[data-testid="mb-reply-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="mb-reply-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="mb-reply-result"].alert-success').isVisible()

		when: 'query headless delivery API for messages under the thread'
		def response = headlessGet(
			"/o/headless-delivery/v1.0/message-board-threads" +
			"/${prereqThreadId}/message-board-messages" +
			'?pageSize=100')

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List

		createdReplyIds.addAll(
			items.collect { it.id as Long }
		)

		then: 'reply count matches (endpoint may include root or only replies)'
		(items.size() == REPLY_COUNT) || (items.size() == REPLY_COUNT + 1)
	}

}
