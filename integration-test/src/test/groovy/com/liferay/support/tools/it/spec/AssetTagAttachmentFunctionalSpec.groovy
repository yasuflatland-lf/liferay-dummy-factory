package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.LdfResourceClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import spock.lang.Shared

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Verifies that AssetTag attachment works correctly for WebContent, Document,
 * MBThread, and MBReply creators. JSONWS-only verification per
 * .claude/rules/testing.md §"Verification strategy: JSONWS only".
 *
 * No @Stepwise — each feature method is independent and self-contained.
 */
class AssetTagAttachmentFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		AssetTagAttachmentFunctionalSpec)

	private static final String DOCUMENT_CLASS_NAME =
		'com.liferay.document.library.kernel.model.DLFileEntry'

	private static final String MB_MESSAGE_CLASS_NAME =
		'com.liferay.message.boards.model.MBMessage'

	private static final String PREREQ_SECTION_TITLE =
		'IT AssetTag Attachment Prereq Section'

	private static final String PREREQ_THREAD_HEADLINE =
		'IT AssetTag Attachment Prereq Thread'

	@Shared
	LdfResourceClient ldf

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	long prereqSectionId

	@Shared
	long prereqThreadId

	// Cleanup lists — best-effort deletion in cleanupSpec
	@Shared
	List<Long> createdDocumentIds = []

	@Shared
	List<Long> createdMBThreadIds = []

	@Shared
	List<Long> createdMBReplyIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)
		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright before headless API calls.
		loginAsAdmin(pw)
		ldf.login()

		// Discover Guest site groupId.
		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long

		// Create prereq MB section (category) for MBThread tests.
		Map section = headlessPost(
			"/o/headless-delivery/v1.0/sites/${guestGroupId}" +
			'/message-board-sections',
			"{\"title\":\"${PREREQ_SECTION_TITLE}\"}")

		prereqSectionId = section.id as Long

		log.info(
			'Created prereq MB section id={} title={}',
			prereqSectionId, section.title)

		// Create prereq MB thread for MBReply tests.
		Map thread = headlessPost(
			"/o/headless-delivery/v1.0/message-board-sections" +
			"/${prereqSectionId}/message-board-threads",
			"{\"headline\":\"${PREREQ_THREAD_HEADLINE}\"," +
			"\"articleBody\":\"Prereq thread body for asset tag tests.\"}")

		prereqThreadId = thread.id as Long

		log.info(
			'Created prereq MB thread id={} headline={}',
			prereqThreadId, thread.headline)
	}

	def cleanupSpec() {
		createdDocumentIds.each { id ->
			try {
				jsonwsPost('dlapp/delete-file-entry', [fileEntryId: id])
			}
			catch (Exception e) {
				log.warn('Failed to delete document {}: {}', id, e.message)
			}
		}

		createdMBReplyIds.each { id ->
			try {
				jsonwsPost('mb.mbmessage/delete-message', [messageId: id])
			}
			catch (Exception e) {
				log.warn('Failed to delete MB reply {}: {}', id, e.message)
			}
		}

		createdMBThreadIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-threads/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to delete MB thread {}: {}', id, e.message)
			}
		}

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

		ldf?.close()
		pw?.close()
	}

	// ---------------------------------------------------------------------------
	// WebContent — WCM response uses custom JSON: ok/totalCreated/totalRequested/perSite
	// classPK is fetched via JSONWS after creation (articles list by groupId).
	// ---------------------------------------------------------------------------

	def 'WebContent batch with empty tags leaves AssetEntry tagNames empty'() {
		given: 'a batch request with no tags (tags field omitted)'
		Map payload = [
			count             : 1,
			baseName          : 'it-wcm-tag-empty',
			groupIds          : [guestGroupId],
			createContentsType: 1,
			titleWords        : 3,
			totalParagraphs   : 1,
			randomAmount      : 0,
			folderId          : 0,
		]

		when: 'POST /ldf/wcm'
		Map response = ldf.post('/ldf/wcm', payload) as Map

		then: 'WCM creator succeeded with per-site result shape'
		assert response.error == null : "creator failed: ${response}"
		response.ok == true
		(response.totalCreated as Integer) == 1
		(response.totalRequested as Integer) == 1

		when: 'look up the article via JSONWS to get its resourcePrimKey (classPK)'
		List articles = jsonwsGet(
			"journal.journalarticle/get-articles" +
			"/group-id/${guestGroupId}/folder-id/0/locale/en_US") as List

		def article = articles?.find { a ->
			(a.title as String)?.contains('it-wcm-tag-empty')
		}

		then: 'article was found'
		assert article != null :
			"No article with baseName 'it-wcm-tag-empty' found: ${articles*.title}"

		when: 'fetch AssetEntry by classPK = resourcePrimKey'
		long classPK = article.resourcePrimKey as Long
		Map entry = _fetchAssetEntry('com.liferay.journal.model.JournalArticle', classPK)

		then: 'identity lock then assert no tags'
		assert (entry.classPK as Long) == classPK :
			"AssetEntry identity mismatch: expected classPK=${classPK}, got ${entry}"
		(entry.tagNames as List).size() == 0
	}

	def 'WebContent batch with non-empty tags attaches exact tag set lowercased and deduped'() {
		given: 'a batch request with tags "Foo,bar,FOO" (case and duplicate test)'
		Map payload = [
			count             : 1,
			baseName          : 'it-wcm-tag-nonempty',
			groupIds          : [guestGroupId],
			createContentsType: 1,
			titleWords        : 3,
			totalParagraphs   : 1,
			randomAmount      : 0,
			folderId          : 0,
			tags              : 'Foo,bar,FOO',
		]

		when: 'POST /ldf/wcm'
		Map response = ldf.post('/ldf/wcm', payload) as Map

		then: 'WCM creator succeeded'
		assert response.error == null : "creator failed: ${response}"
		response.ok == true
		(response.totalCreated as Integer) == 1

		when: 'look up the article via JSONWS'
		List articles = jsonwsGet(
			"journal.journalarticle/get-articles" +
			"/group-id/${guestGroupId}/folder-id/0/locale/en_US") as List

		def article = articles?.find { a ->
			(a.title as String)?.contains('it-wcm-tag-nonempty')
		}

		then: 'article was found'
		assert article != null :
			"No article with baseName 'it-wcm-tag-nonempty' found: ${articles*.title}"

		when: 'fetch AssetEntry'
		long classPK = article.resourcePrimKey as Long
		Map entry = _fetchAssetEntry('com.liferay.journal.model.JournalArticle', classPK)

		then: 'identity lock'
		assert (entry.classPK as Long) == classPK :
			"AssetEntry identity mismatch: expected classPK=${classPK}, got ${entry}"

		and: 'exact tag set — lowercased + deduped (Foo,bar,FOO → {foo,bar})'
		assert (entry.tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${entry.tagNames}"
	}

	// ---------------------------------------------------------------------------
	// Document — items have fileEntryId; classPK for AssetEntry is fileEntryId
	// DocumentCreator creates .txt files when uploadedFiles is empty.
	// ---------------------------------------------------------------------------

	def 'Document batch with empty tags leaves AssetEntry tagNames empty'() {
		given: 'a batch request with no tags'
		Map payload = [
			count        : 1,
			baseName     : 'it-doc-tag-empty',
			groupId      : guestGroupId,
			folderId     : 0,
			description  : 'Asset tag integration test document (empty tags)',
			uploadedFiles: '',
		]

		when: 'POST /ldf/doc'
		Map response = ldf.post('/ldf/doc', payload) as Map

		then: 'creator succeeded with full response shape'
		assert response.error == null : "creator failed: ${response}"
		response.success == true
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')
		(response.count as Integer) == 1
		(response.items as List).size() == 1

		when: 'fetch AssetEntry for the created document'
		long fileEntryId = (response.items[0] as Map).fileEntryId as Long
		Map entry = _fetchAssetEntry(DOCUMENT_CLASS_NAME, fileEntryId)

		then: 'identity lock then assert no tags'
		assert (entry.classPK as Long) == fileEntryId :
			"AssetEntry identity mismatch: expected classPK=${fileEntryId}, got ${entry}"
		(entry.tagNames as List).size() == 0

		cleanup:
		if (fileEntryId > 0) {
			createdDocumentIds << fileEntryId
		}
	}

	def 'Document batch with non-empty tags attaches exact tag set lowercased and deduped'() {
		given: 'a batch request with tags "Foo,bar,FOO"'
		Map payload = [
			count        : 1,
			baseName     : 'it-doc-tag-nonempty',
			groupId      : guestGroupId,
			folderId     : 0,
			description  : 'Asset tag integration test document (non-empty tags)',
			uploadedFiles: '',
			tags         : 'Foo,bar,FOO',
		]

		when: 'POST /ldf/doc'
		Map response = ldf.post('/ldf/doc', payload) as Map

		then: 'creator succeeded'
		assert response.error == null : "creator failed: ${response}"
		response.success == true
		(response.count as Integer) == 1
		(response.items as List).size() == 1

		when: 'fetch AssetEntry for the document'
		long fileEntryId = (response.items[0] as Map).fileEntryId as Long
		Map entry = _fetchAssetEntry(DOCUMENT_CLASS_NAME, fileEntryId)

		then: 'identity lock'
		assert (entry.classPK as Long) == fileEntryId :
			"AssetEntry identity mismatch: expected classPK=${fileEntryId}, got ${entry}"

		and: 'exact tag set — lowercased + deduped'
		assert (entry.tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${entry.tagNames}"

		cleanup:
		if (fileEntryId > 0) {
			createdDocumentIds << fileEntryId
		}
	}

	// ---------------------------------------------------------------------------
	// MBThread — items have messageId, threadId; AssetEntry classPK = messageId
	// ---------------------------------------------------------------------------

	def 'MBThread batch with empty tags leaves AssetEntry tagNames empty'() {
		given: 'a batch request with no tags'
		Map payload = [
			count     : 1,
			baseName  : 'it-mbthread-tag-empty',
			groupId   : guestGroupId,
			categoryId: prereqSectionId,
			body      : 'Asset tag integration test body (empty tags).',
			format    : 'html',
		]

		when: 'POST /ldf/mb-thread'
		Map response = ldf.post('/ldf/mb-thread', payload) as Map

		then: 'creator succeeded with full response shape'
		assert response.error == null : "creator failed: ${response}"
		response.success == true
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')
		(response.count as Integer) == 1
		(response.items as List).size() == 1

		when: 'fetch AssetEntry for the created message (classPK = messageId)'
		long messageId = (response.items[0] as Map).messageId as Long
		Map entry = _fetchAssetEntry(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock then assert no tags'
		assert (entry.classPK as Long) == messageId :
			"AssetEntry identity mismatch: expected classPK=${messageId}, got ${entry}"
		(entry.tagNames as List).size() == 0

		cleanup:
		Long threadId = (response?.items?.getAt(0) as Map)?.threadId as Long
		if (threadId) {
			createdMBThreadIds << threadId
		}
	}

	def 'MBThread batch with non-empty tags attaches exact tag set lowercased and deduped'() {
		given: 'a batch request with tags "Foo,bar,FOO"'
		Map payload = [
			count     : 1,
			baseName  : 'it-mbthread-tag-nonempty',
			groupId   : guestGroupId,
			categoryId: prereqSectionId,
			body      : 'Asset tag integration test body (non-empty tags).',
			format    : 'html',
			tags      : 'Foo,bar,FOO',
		]

		when: 'POST /ldf/mb-thread'
		Map response = ldf.post('/ldf/mb-thread', payload) as Map

		then: 'creator succeeded'
		assert response.error == null : "creator failed: ${response}"
		response.success == true
		(response.count as Integer) == 1
		(response.items as List).size() == 1

		when: 'fetch AssetEntry'
		long messageId = (response.items[0] as Map).messageId as Long
		Map entry = _fetchAssetEntry(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock'
		assert (entry.classPK as Long) == messageId :
			"AssetEntry identity mismatch: expected classPK=${messageId}, got ${entry}"

		and: 'exact tag set — lowercased + deduped'
		assert (entry.tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${entry.tagNames}"

		cleanup:
		Long threadId = (response?.items?.getAt(0) as Map)?.threadId as Long
		if (threadId) {
			createdMBThreadIds << threadId
		}
	}

	// ---------------------------------------------------------------------------
	// MBReply — items have messageId; AssetEntry classPK = messageId
	// Uses the prereq thread created in setupSpec.
	// ---------------------------------------------------------------------------

	def 'MBReply batch with empty tags leaves AssetEntry tagNames empty'() {
		given: 'a batch request with no tags'
		Map payload = [
			count      : 1,
			baseName   : 'it-mbreply-tag-empty',
			threadId   : prereqThreadId,
			body       : 'Asset tag integration test reply (empty tags).',
			format     : 'html',
			fakerEnable: false,
		]

		when: 'POST /ldf/mb-reply'
		Map response = ldf.post('/ldf/mb-reply', payload) as Map

		then: 'creator succeeded with full response shape'
		assert response.error == null : "creator failed: ${response}"
		response.success == true
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')
		(response.count as Integer) == 1
		(response.items as List).size() == 1

		when: 'fetch AssetEntry for the reply message (classPK = messageId)'
		long messageId = (response.items[0] as Map).messageId as Long
		Map entry = _fetchAssetEntry(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock then assert no tags'
		assert (entry.classPK as Long) == messageId :
			"AssetEntry identity mismatch: expected classPK=${messageId}, got ${entry}"
		(entry.tagNames as List).size() == 0

		cleanup:
		Long mid = (response?.items?.getAt(0) as Map)?.messageId as Long
		if (mid) {
			createdMBReplyIds << mid
		}
	}

	def 'MBReply batch with non-empty tags attaches exact tag set lowercased and deduped'() {
		given: 'a batch request with tags "Foo,bar,FOO"'
		Map payload = [
			count      : 1,
			baseName   : 'it-mbreply-tag-nonempty',
			threadId   : prereqThreadId,
			body       : 'Asset tag integration test reply (non-empty tags).',
			format     : 'html',
			fakerEnable: false,
			tags       : 'Foo,bar,FOO',
		]

		when: 'POST /ldf/mb-reply'
		Map response = ldf.post('/ldf/mb-reply', payload) as Map

		then: 'creator succeeded'
		assert response.error == null : "creator failed: ${response}"
		response.success == true
		(response.count as Integer) == 1
		(response.items as List).size() == 1

		when: 'fetch AssetEntry'
		long messageId = (response.items[0] as Map).messageId as Long
		Map entry = _fetchAssetEntry(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock'
		assert (entry.classPK as Long) == messageId :
			"AssetEntry identity mismatch: expected classPK=${messageId}, got ${entry}"

		and: 'exact tag set — lowercased + deduped'
		assert (entry.tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${entry.tagNames}"

		cleanup:
		Long mid = (response?.items?.getAt(0) as Map)?.messageId as Long
		if (mid) {
			createdMBReplyIds << mid
		}
	}

	// ---------------------------------------------------------------------------
	// In-file helpers (per .claude/rules/testing.md §"Helper extraction stays in-file")
	// ---------------------------------------------------------------------------

	/**
	 * Fetch AssetEntry by className and classPK via JSONWS.
	 * The JSONWS method assetentry/get-entry accepts className + classPK as POST params.
	 */
	private Map _fetchAssetEntry(String className, long classPK) {
		return jsonwsPost('assetentry/get-entry', [
			className: className,
			classPK  : classPK,
		]) as Map
	}

}
