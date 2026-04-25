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

	private static final String WEBCONTENT_CLASS_NAME =
		'com.liferay.journal.model.JournalArticle'

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
		// Container is disposable (withReuse(false)); explicit JSONWS/headless cleanup
		// is unnecessary and best-effort try/catch is banned by testing.md §Cleanup.
		ldf?.close()
		pw?.close()
	}

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
		response.containsKey('perSite')
		(response.perSite as List).size() == 1

		when: 'fetch the most-recently-modified article in the guest group'
		List articles = jsonwsPost('journal.journalarticle/get-articles', [
			groupId : guestGroupId,
			folderId: 0,
			locale  : 'en_US',
		]) as List

		assert articles != null && !articles.isEmpty() :
			"journal.journalarticle/get-articles returned empty for group ${guestGroupId}"

		articles = articles.sort { a, b ->
			(b.modifiedDate as Long) <=> (a.modifiedDate as Long)
		}

		Map article = articles[0] as Map
		long classPK = (article.resourcePrimKey as Long)

		then: 'article was found and classPK is valid'
		assert classPK > 0 :
			"resourcePrimKey was not positive; article=${article}"

		when: 'fetch asset tags via assettag/get-tags'
		List<String> tagNames = _fetchAssetTagNames(WEBCONTENT_CLASS_NAME, classPK)

		then: 'no tags attached'
		assert tagNames.size() == 0 :
			"expected empty tagNames, got: ${tagNames}"
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
		response.containsKey('perSite')
		(response.perSite as List).size() == 1

		when: 'fetch the most-recently-modified article in the guest group'
		List articles = jsonwsPost('journal.journalarticle/get-articles', [
			groupId : guestGroupId,
			folderId: 0,
			locale  : 'en_US',
		]) as List

		assert articles != null && !articles.isEmpty() :
			"journal.journalarticle/get-articles returned empty for group ${guestGroupId}"

		articles = articles.sort { a, b ->
			(b.modifiedDate as Long) <=> (a.modifiedDate as Long)
		}

		Map article = articles[0] as Map
		long classPK = (article.resourcePrimKey as Long)

		then: 'article was found and classPK is valid'
		assert classPK > 0 :
			"resourcePrimKey was not positive; article=${article}"

		when: 'fetch asset tags via assettag/get-tags'
		List<String> tagNames = _fetchAssetTagNames(WEBCONTENT_CLASS_NAME, classPK)

		then: 'exact tag set — lowercased + deduped (Foo,bar,FOO → {foo,bar})'
		assert (tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${tagNames}"
	}

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

		when: 'fetch asset tags for the created document'
		long fileEntryId = (response.items[0] as Map).fileEntryId as Long
		List<String> tagNames = _fetchAssetTagNames(DOCUMENT_CLASS_NAME, fileEntryId)

		then: 'identity lock then assert no tags'
		assert fileEntryId > 0 :
			"fileEntryId not positive: ${response.items[0]}"
		assert tagNames.size() == 0 :
			"expected empty tagNames, got: ${tagNames}"
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

		when: 'fetch asset tags for the document'
		long fileEntryId = (response.items[0] as Map).fileEntryId as Long
		List<String> tagNames = _fetchAssetTagNames(DOCUMENT_CLASS_NAME, fileEntryId)

		then: 'identity lock'
		assert fileEntryId > 0 :
			"fileEntryId not positive: ${response.items[0]}"

		and: 'exact tag set — lowercased + deduped'
		assert (tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${tagNames}"
	}

	def 'MBThread batch with empty tags leaves AssetEntry tagNames empty'() {
		given: 'prereq ids are populated'
		assert prereqSectionId > 0 : 'setupSpec failed to populate prereqSectionId'

		and: 'a batch request with no tags'
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

		when: 'fetch asset tags for the created message (classPK = messageId)'
		long messageId = (response.items[0] as Map).messageId as Long
		List<String> tagNames = _fetchAssetTagNames(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock then assert no tags'
		assert messageId > 0 :
			"messageId not positive: ${response.items[0]}"
		assert tagNames.size() == 0 :
			"expected empty tagNames, got: ${tagNames}"
	}

	def 'MBThread batch with non-empty tags attaches exact tag set lowercased and deduped'() {
		given: 'prereq ids are populated'
		assert prereqSectionId > 0 : 'setupSpec failed to populate prereqSectionId'

		and: 'a batch request with tags "Foo,bar,FOO"'
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

		when: 'fetch asset tags for the message'
		long messageId = (response.items[0] as Map).messageId as Long
		List<String> tagNames = _fetchAssetTagNames(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock'
		assert messageId > 0 :
			"messageId not positive: ${response.items[0]}"

		and: 'exact tag set — lowercased + deduped'
		assert (tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${tagNames}"
	}

	def 'MBReply batch with empty tags leaves AssetEntry tagNames empty'() {
		given: 'prereq ids are populated'
		assert prereqSectionId > 0 : 'setupSpec failed to populate prereqSectionId'
		assert prereqThreadId > 0 : 'setupSpec failed to populate prereqThreadId'

		and: 'a batch request with no tags'
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

		when: 'fetch asset tags for the reply message (classPK = messageId)'
		long messageId = (response.items[0] as Map).messageId as Long
		List<String> tagNames = _fetchAssetTagNames(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock then assert no tags'
		assert messageId > 0 :
			"messageId not positive: ${response.items[0]}"
		assert tagNames.size() == 0 :
			"expected empty tagNames, got: ${tagNames}"
	}

	def 'MBReply batch with non-empty tags attaches exact tag set lowercased and deduped'() {
		given: 'prereq ids are populated'
		assert prereqSectionId > 0 : 'setupSpec failed to populate prereqSectionId'
		assert prereqThreadId > 0 : 'setupSpec failed to populate prereqThreadId'

		and: 'a batch request with tags "Foo,bar,FOO"'
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

		when: 'fetch asset tags for the reply message'
		long messageId = (response.items[0] as Map).messageId as Long
		List<String> tagNames = _fetchAssetTagNames(MB_MESSAGE_CLASS_NAME, messageId)

		then: 'identity lock'
		assert messageId > 0 :
			"messageId not positive: ${response.items[0]}"

		and: 'exact tag set — lowercased + deduped'
		assert (tagNames as Set) == (['foo', 'bar'] as Set) :
			"expected exact tag set {foo, bar}, got: ${tagNames}"
	}

	private List<String> _fetchAssetTagNames(String className, long classPK) {
		List tags = jsonwsPost('assettag/get-tags', [
			className: className,
			classPK  : classPK,
		]) as List

		return (tags ?: []).collect { (it.name as String) }
	}

}
