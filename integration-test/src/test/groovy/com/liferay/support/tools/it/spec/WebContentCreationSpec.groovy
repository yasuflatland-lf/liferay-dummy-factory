package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.JsonwsSetupHelper
import com.liferay.support.tools.it.util.LdfResourceClient

import spock.lang.Shared

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebContentCreationSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		WebContentCreationSpec)

	private static final String DDM_DEFINITION = '''{
		"availableLanguageIds": ["en_US"],
		"defaultLanguageId": "en_US",
		"fields": [{
			"dataType": "string",
			"fieldNamespace": "",
			"indexType": "keyword",
			"label": {"en_US": "Body"},
			"localizable": true,
			"name": "body",
			"readOnly": false,
			"repeatable": false,
			"required": false,
			"showLabel": true,
			"type": "text"
		}]
	}'''

	private static final String DDM_TEMPLATE_SCRIPT =
		'<div>${body.getData()}</div>'

	@Shared
	LdfResourceClient ldf

	@Shared
	JsonwsSetupHelper jsonws

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)

		// Log in the admin session so JsonwsSetupHelper (which uses Basic Auth against
		// JSONWS) runs under an already-primed cookie store. D2 removed the
		// password-change detour; this is now a one-shot login.
		ldf.login()

		jsonws = new JsonwsSetupHelper(liferay.baseUrl)
	}

	def cleanupSpec() {
		jsonws?.cleanupAll()
		ldf?.close()
	}

	def 'creates articles with simple content type in a single site'() {
		given:
		Map site = jsonws.createSite(
			"WcmSimpleSingle-${System.nanoTime()}")
		Long siteId = site.groupId as Long

		when:
		Map response = ldf.createWebContent([
			count: 3,
			baseName: 'Hello',
			groupIds: [siteId],
			createContentsType: '0',
			baseArticle: 'Sample body text',
			folderId: 0
		])

		then:
		response.ok == true
		response.totalCreated == 3

		and:
		int articleCount = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${siteId}/folder-id/0") as int

		articleCount == 3
	}

	def 'creates articles in multiple sites and reports per-site success'() {
		given:
		Map siteA = jsonws.createSite(
			"WcmMultiA-${System.nanoTime()}")
		Map siteB = jsonws.createSite(
			"WcmMultiB-${System.nanoTime()}")

		Long siteAId = siteA.groupId as Long
		Long siteBId = siteB.groupId as Long

		when:
		Map response = ldf.createWebContent([
			count: 5,
			baseName: 'MultiSite',
			groupIds: [siteAId, siteBId],
			createContentsType: '0',
			baseArticle: 'Sample body',
			folderId: 0
		])

		then: 'response reports totals and per-site success'
		response.ok == true
		response.totalRequested == 10
		response.totalCreated == 10

		and: 'perSite list has two entries, each with created=5, failed=0'
		List perSite = response.perSite as List
		perSite.size() == 2

		Map entryA = perSite.find { (it.groupId as Long) == siteAId } as Map
		Map entryB = perSite.find { (it.groupId as Long) == siteBId } as Map

		entryA != null
		(entryA.created as int) == 5
		(entryA.failed as int) == 0

		entryB != null
		(entryB.created as int) == 5
		(entryB.failed as int) == 0

		and: 'JSONWS confirms 5 articles in each site'
		int countA = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${siteAId}/folder-id/0") as int
		int countB = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${siteBId}/folder-id/0") as int

		countA == 5
		countB == 5
	}

	def 'creates articles with random content generator'() {
		given:
		Map site = jsonws.createSite(
			"WcmRandom-${System.nanoTime()}")
		Long siteId = site.groupId as Long

		when:
		Map response = ldf.createWebContent([
			count: 2,
			baseName: 'Random',
			groupIds: [siteId],
			createContentsType: '1',
			titleWords: 5,
			totalParagraphs: 3,
			randomAmount: 0,
			linkLists: '',
			folderId: 0
		])

		then: 'response reports both articles created'
		response.ok == true
		response.totalCreated == 2

		and: 'JSONWS reports 2 articles in the site'
		int articleCount = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${siteId}/folder-id/0") as int

		articleCount == 2

		and: 'each article content carries at least 3 paragraph lines'
		List articles = jsonwsGet(
			"journal.journalarticle/get-articles" +
			"/group-id/${siteId}/folder-id/0/locale/en_US") as List

		articles.size() == 2

		articles.every { article ->
			String content = (article.content as String) ?: ''

			log.info(
				'Random article {} content length={}', article.articleId,
				content.length())

			int paragraphLines = _countParagraphLines(content)

			paragraphLines >= 3
		}
	}

	def 'reports per-site failure when structure id is missing'() {
		// DDMStructureService#addStructure does not accept a raw definition
		// JSON via JSONWS form-encoded POST on Liferay (it expects a
		// serialized DDMForm Java object), so we cannot cheaply build a
		// site-scoped structure for this test. Instead, we pass a
		// deliberately nonexistent ddmStructureId; the WebContentCreator
		// reports per-site failure for both target sites and the overall
		// payload flags the batch as not-ok with totalCreated=0. This still
		// exercises the per-site error pipeline end to end.
		given:
		Map siteA = jsonws.createSite(
			"WcmStructA-${System.nanoTime()}")
		Map siteB = jsonws.createSite(
			"WcmStructB-${System.nanoTime()}")

		Long siteAId = siteA.groupId as Long
		Long siteBId = siteB.groupId as Long

		when:
		Map response = ldf.createWebContent([
			count: 3,
			baseName: 'Struct',
			groupIds: [siteAId, siteBId],
			createContentsType: '2',
			ddmStructureId: 999_999_999,
			ddmTemplateId: 999_999_999,
			folderId: 0
		])

		then: 'overall response is not ok and nothing was created'
		response.ok == false
		response.totalRequested == 6
		response.totalCreated == 0

		and: 'JSONWS shows both sites have zero articles'
		int countA = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${siteAId}/folder-id/0") as int
		int countB = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${siteBId}/folder-id/0") as int

		countA == 0
		countB == 0

		and: 'perSite payload reports a non-empty error for each site'
		List perSite = response.perSite as List
		perSite.size() == 2

		Map entryA = perSite.find { (it.groupId as Long) == siteAId } as Map
		Map entryB = perSite.find { (it.groupId as Long) == siteBId } as Map

		entryA != null
		(entryA.failed as int) == 3
		(entryA.error as String)?.trim()

		entryB != null
		(entryB.failed as int) == 3
		(entryB.error as String)?.trim()
	}

	private static int _countParagraphLines(String content) {
		if ((content == null) || content.isEmpty()) {
			return 0
		}

		int count = 0

		for (String line : content.split('\n')) {
			if ((line != null) && !line.trim().isEmpty()) {
				count++
			}
		}

		return count
	}

}
