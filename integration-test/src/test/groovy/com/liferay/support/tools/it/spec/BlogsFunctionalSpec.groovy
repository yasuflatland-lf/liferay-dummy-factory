package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.LdfResourceClient

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class BlogsFunctionalSpec extends BaseLiferaySpec {

	private static final String RUN_SUFFIX =
		String.valueOf(System.currentTimeMillis())

	private static final String BASE_BLOG_TITLE = "ITBlog${RUN_SUFFIX}"
	private static final int BLOG_COUNT = 3

	@Shared
	LdfResourceClient ldf

	@Shared
	long guestGroupId

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)
		ldf.login()

		def group = jsonwsGet(
			"group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long
	}

	def cleanupSpec() {
		// withReuse(false) starts a fresh container per run; no cleanup needed.
		ldf?.close()
	}

	def 'creates blog entries with sequential titles'() {
		given:
		Map fields = [
			count: BLOG_COUNT,
			baseName: BASE_BLOG_TITLE,
			groupId: guestGroupId,
			content: '<p>Integration test blog content</p>'
		]

		when: 'POST /ldf/blog'
		Map response = ldf.createBlog(fields)

		then: 'creator reports success with correct counts'
		response.success == true
		(response.count as Integer) == BLOG_COUNT
		(response.requested as Integer) == BLOG_COUNT
		(response.items as List) != null
		(response.items as List).size() == BLOG_COUNT

		and: 'each item has entryId and title'
		(response.items as List).every { item ->
			Map entry = item as Map
			entry.entryId != null && entry.title != null
		}
	}

	def 'created entries exist in Liferay via JSONWS'() {
		when: 'query blog entries from Guest site via JSONWS'
		def entries = jsonwsGet(
			'blogs.blogsentry/get-group-entries' +
			"?groupId=${guestGroupId}&status=0&max=100") as List

		then: 'response is not null'
		entries != null

		when: 'filter entries matching our base title'
		def matchingEntries = entries.findAll { entry ->
			(entry.title as String)?.startsWith(BASE_BLOG_TITLE)
		}

		then: 'all created blog entries exist'
		matchingEntries.size() == BLOG_COUNT
	}

	def 'response contract fields are present on success path'() {
		given:
		String uniqueTitle = "ITBlogContract${RUN_SUFFIX}"

		Map fields = [
			count: 1,
			baseName: uniqueTitle,
			groupId: guestGroupId,
			content: '<p>Contract test</p>'
		]

		when: 'POST /ldf/blog with count=1'
		Map response = ldf.createBlog(fields)

		then: 'all contract fields are present'
		response.containsKey('success')
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')

		and: 'contract values are correct'
		response.success == true
		(response.count as Integer) == 1
		(response.requested as Integer) == 1
		(response.skipped as Integer) == 0
		(response.items as List).size() == 1
	}

}
