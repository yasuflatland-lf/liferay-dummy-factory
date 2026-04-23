package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.liferay.support.tools.it.util.WorkflowHttpClient

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class WorkflowHttpE2ESpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		WorkflowHttpE2ESpec)

	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	private static final String ROLE_BASE_NAME = "WFHttpSiteRole${RUN_SUFFIX}"
	private static final String SITE_BASE_NAME = "WFHttpSite${RUN_SUFFIX}"
	private static final String USER_BASE_NAME = "WFHttpUser${RUN_SUFFIX}"
	private static final String VOCAB_BASE_NAME = "WFHttpVocab${RUN_SUFFIX}"
	private static final String WEB_CONTENT_BASE_NAME =
		"WFHttpArticle${RUN_SUFFIX}"
	private static final String DOC_BASE_NAME = "WFHttpDoc${RUN_SUFFIX}"
	private static final String CATEGORY_BASE_NAME =
		"WFHttpCategory${RUN_SUFFIX}"
	private static final String MB_CATEGORY_BASE_NAME =
		"WFHttpMBSection${RUN_SUFFIX}"
	private static final String MB_THREAD_BASE_NAME =
		"WFHttpMBThread${RUN_SUFFIX}"
	private static final String PAGE_BASE_NAME = "WFHttpPage${RUN_SUFFIX}"
	private static final String BLOG_BASE_NAME = "WFHttpBlog${RUN_SUFFIX}"

	@Shared
	PlaywrightLifecycle pw

	@Shared
	WorkflowHttpClient workflowHttpClient

	@Shared
	Long createdGroupId

	@Shared
	Long createdRoleId

	@Shared
	Long createdUserId

	@Shared
	Long createdVocabularyId

	@Shared
	Long createdCategoryId

	@Shared
	Long createdMbCategoryId

	@Shared
	Long createdThreadId

	@Shared
	Long createdPlid

	@Shared
	Long createdFileEntryId

	@Shared
	Long createdBlogEntryId

	private static final List<String> EXPECTED_OPERATIONS = [
		'blogs.create',
		'category.create',
		'document.create',
		'layout.create',
		'mbCategory.create',
		'mbThread.create',
		'role.create',
		'site.create',
		'user.create',
		'vocabulary.create',
		'webContent.create'
	]

	private static final List<String> READY_OPERATIONS = [
		'blogs.create',
		'document.create',
		'layout.create',
		'mbCategory.create',
		'mbThread.create',
		'role.create',
		'site.create',
		'user.create',
		'webContent.create'
	]

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		loginAsAdmin(pw)

		workflowHttpClient = new WorkflowHttpClient(liferay.baseUrl, pw.page)

		_waitForWorkflowOperations()
	}

	def cleanupSpec() {
		if (createdGroupId) {
			try {
				jsonwsPost(
					'group/delete-group',
					['groupId': createdGroupId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up site {}: {}', createdGroupId, e.message)
			}
		}

		if (createdUserId) {
			try {
				jsonwsPost(
					'user/delete-user',
					['userId': createdUserId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up user {}: {}', createdUserId, e.message)
			}
		}

		if (createdRoleId) {
			try {
				jsonwsPost('role/delete-role', ['roleId': createdRoleId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up role {}: {}', createdRoleId, e.message)
			}
		}

		if (createdVocabularyId) {
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/" +
					"${createdVocabularyId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up vocabulary {}: {}',
					createdVocabularyId, e.message)
			}
		}

		workflowHttpClient = null
		pw?.close()
	}

	def 'plan exposes the extended workflow steps in order'() {
		when:
		Map response = workflowHttpClient.plan(_workflowRequest())

		then:
		response.errors == []

		and:
		List<Map<String, Object>> steps = (
			(response.plan as Map).definition as Map
		).steps as List<Map<String, Object>>

		steps*.id == [
			'createSite',
			'createRole',
			'createUser',
			'createVocabulary',
			'createWebContent',
			'createDocument',
			'createCategory',
			'createMBCategory',
			'createMBThread',
			'createLayout',
			'createBlog'
		]

		steps*.operation == [
			'site.create',
			'role.create',
			'user.create',
			'vocabulary.create',
			'webContent.create',
			'document.create',
			'category.create',
			'mbCategory.create',
			'mbThread.create',
			'layout.create',
			'blogs.create'
		]
	}

	def 'execute returns nested ids, counts, and dependencies through HTTP JSON'() {
		when:
		Map response = workflowHttpClient.execute(_workflowRequest())

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps*.stepId == [
			'createSite',
			'createRole',
			'createUser',
			'createVocabulary',
			'createWebContent',
			'createDocument',
			'createCategory',
			'createMBCategory',
			'createMBThread',
			'createLayout',
			'createBlog'
		]
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map siteStep = _stepById(steps, 'createSite')
		Map roleStep = _stepById(steps, 'createRole')
		Map userStep = _stepById(steps, 'createUser')
		Map vocabularyStep = _stepById(steps, 'createVocabulary')
		Map webContentStep = _stepById(steps, 'createWebContent')
		Map documentStep = _stepById(steps, 'createDocument')
		Map categoryStep = _stepById(steps, 'createCategory')
		Map mbCategoryStep = _stepById(steps, 'createMBCategory')
		Map mbThreadStep = _stepById(steps, 'createMBThread')
		Map layoutStep = _stepById(steps, 'createLayout')
		Map blogStep = _stepById(steps, 'createBlog')

		Map siteResult = siteStep.result as Map
		Map roleResult = roleStep.result as Map
		Map userResult = userStep.result as Map
		Map vocabularyResult = vocabularyStep.result as Map
		Map webContentResult = webContentStep.result as Map
		Map documentResult = documentStep.result as Map
		Map categoryResult = categoryStep.result as Map
		Map mbCategoryResult = mbCategoryStep.result as Map
		Map mbThreadResult = mbThreadStep.result as Map
		Map layoutResult = layoutStep.result as Map
		Map blogResult = blogStep.result as Map

		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map roleItem = (roleResult.items as List<Map<String, Object>>).first()
		Map userItem = (userResult.items as List<Map<String, Object>>).first()
		Map vocabularyItem =
			(vocabularyResult.items as List<Map<String, Object>>).first()
		Map webContentItem =
			(webContentResult.items as List<Map<String, Object>>).first()
		Map documentItem =
			(documentResult.items as List<Map<String, Object>>).first()
		Map categoryItem =
			(categoryResult.items as List<Map<String, Object>>).first()
		Map mbCategoryItem =
			(mbCategoryResult.items as List<Map<String, Object>>).first()
		Map mbThreadItem =
			(mbThreadResult.items as List<Map<String, Object>>).first()
		Map layoutItem = (layoutResult.items as List<Map<String, Object>>).first()
		Map blogItem = (blogResult.items as List<Map<String, Object>>).first()

		createdGroupId = siteItem.groupId as Long
		createdRoleId = roleItem.roleId as Long
		createdUserId = userItem.userId as Long
		createdVocabularyId = vocabularyItem.vocabularyId as Long
		createdCategoryId = categoryItem.categoryId as Long
		createdMbCategoryId = mbCategoryItem.categoryId as Long
		createdThreadId = mbThreadItem.threadId as Long
		createdPlid = layoutItem.plid as Long
		createdFileEntryId = documentItem.fileEntryId as Long
		createdBlogEntryId = blogItem.entryId as Long

		then:
		(siteResult.count as Number) == 1
		(siteItem.groupId as Long) > 0

		and:
		(roleResult.count as Number) == 1
		(roleItem.roleId as Long) > 0

		and:
		(userResult.count as Number) == 1
		(userItem.userId as Long) > 0

		and:
		(vocabularyResult.count as Number) == 1
		(vocabularyItem.vocabularyId as Long) > 0

		and:
		(webContentResult.count as Number) == 1
		(webContentItem.created as Number) == 1
		(webContentItem.groupId as Long) == createdGroupId

		and:
		(documentResult.count as Number) == 1
		(documentItem.fileEntryId as Long) > 0

		and:
		(categoryResult.count as Number) == 1
		(categoryItem.vocabularyId as Long) == createdVocabularyId

		and:
		(mbCategoryResult.count as Number) == 1
		(mbCategoryItem.groupId as Long) == createdGroupId

		and:
		(mbThreadResult.count as Number) == 1
		(mbThreadItem.threadId as Long) > 0

		and:
		(layoutResult.count as Number) == 1
		(layoutItem.plid as Long) > 0

		and:
		(blogResult.count as Number) == 1
		(blogItem.entryId as Long) > 0

		and:
		Map createdUser = jsonwsGet(
			"user/get-user-by-id/user-id/${createdUserId}") as Map
		createdUser.userId as Long == createdUserId
		(createdUser.screenName as String) == "${USER_BASE_NAME.toLowerCase()}1"

		and:
		List siteGroups = jsonwsGet(
			"group/get-user-sites-groups" +
			"/user-id/${createdUserId}/start/-1/end/-1") as List
		siteGroups.any { (it.groupId as Long) == createdGroupId }

		and:
		List siteUserGroupRoles = jsonwsGet(
			"role/get-user-group-roles" +
			"/user-id/${createdUserId}/group-id/${createdGroupId}") as List
		siteUserGroupRoles.any {
			(it.roleId as Long) == createdRoleId
		}

		and:
		Map createdSite = jsonwsGet(
			"group/get-group/group-id/${createdGroupId}") as Map
		createdSite.groupId as Long == createdGroupId

		and:
		List vocabularies = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/sites/${createdGroupId}" +
			'/taxonomy-vocabularies?pageSize=100').items as List
		vocabularies.any { (it.id as Long) == createdVocabularyId }

		and:
		List categories = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/" +
			"${createdVocabularyId}/taxonomy-categories?pageSize=100").items as List
		categories.any { (it.id as Long) == createdCategoryId }

		and:
		List mbCategories = headlessGet(
			"/o/headless-delivery/v1.0/sites/${createdGroupId}" +
			"/message-board-sections?pageSize=100").items as List
		mbCategories.any { (it.id as Long) == createdMbCategoryId }

		and:
		List mbThreads = headlessGet(
			"/o/headless-delivery/v1.0/message-board-sections/" +
			"${createdMbCategoryId}/message-board-threads?pageSize=100").items as List
		mbThreads.any { (it.id as Long) == createdThreadId }

		and:
		List layouts = jsonwsGet(
			"layout/get-layouts/group-id/${createdGroupId}" +
			'/private-layout/false') as List
		layouts.any { layout ->
			(layout.plid as Long) == createdPlid &&
				(layout.friendlyURL as String)?.startsWith(
					"/${PAGE_BASE_NAME.toLowerCase()}")
		}

		and:
		List documents = jsonwsGet(
			"dlapp/get-file-entries/repository-id/${createdGroupId}" +
			'/folder-id/0') as List
		documents.any {
			(it.fileEntryId as Long) == createdFileEntryId &&
				(it.title as String)?.startsWith(DOC_BASE_NAME)
		}

		and:
		int articleCount = jsonwsGet(
			"journal.journalarticle/get-articles-count" +
			"/group-id/${createdGroupId}/folder-id/0") as int
		articleCount == 1

		and:
		List blogEntries = jsonwsGet(
			'blogs.blogsentry/get-group-entries' +
			"?groupId=${createdGroupId}&status=0&max=100") as List
		blogEntries.any {
			(it.entryId as Long) == createdBlogEntryId &&
				(it.title as String)?.startsWith(BLOG_BASE_NAME)
		}
	}

	private void _waitForWorkflowOperations() {
		long deadline = System.currentTimeMillis() + 120_000L
		Exception lastException = null

		while (System.currentTimeMillis() < deadline) {
			try {
				Map response = workflowHttpClient.functions()
				List<Map<String, Object>> functions =
					(response.functions as List<Map<String, Object>>) ?: List.of()
				List<String> operations = functions.collect {
					it.operation as String
				}

				if (operations.containsAll(READY_OPERATIONS)) {
					return
				}

				lastException = new IllegalStateException(
					"Missing workflow operations: " +
						(READY_OPERATIONS - operations).join(', '))
			}
			catch (Exception e) {
				lastException = e
			}

			Thread.sleep(2_000)
		}

		throw new IllegalStateException(
			'Workflow operations did not become available before timeout',
			lastException)
	}

	private static Map<String, Object> _stepById(
		List<Map<String, Object>> steps, String stepId) {

		Map<String, Object> step = steps.find {
			(it.stepId as String) == stepId
		} as Map<String, Object>

		assert step != null

		return step
	}

	private Map<String, Object> _workflowRequest() {
		return [
			schemaVersion: '1.0',
			workflowId   : "workflow-http-e2e-${RUN_SUFFIX}",
			input        : [:],
				steps        : [
					[
						id             : 'createSite',
						idempotencyKey : "site-${RUN_SUFFIX}",
						operation      : 'site.create',
						onError        : [policy: 'FAIL_FAST'],
						params         : [
							[name: 'count', value: 1],
							[name: 'baseName', value: SITE_BASE_NAME],
							[name: 'membershipType', value: 'open'],
							[name: 'manualMembership', value: true]
						]
					],
					[
						id             : 'createRole',
						idempotencyKey : "role-${RUN_SUFFIX}",
						operation      : 'role.create',
						onError        : [policy: 'FAIL_FAST'],
						params         : [
							[name: 'count', value: 1],
							[name: 'baseName', value: ROLE_BASE_NAME],
							[name: 'roleType', value: 'site']
						]
					],
					[
						id             : 'createUser',
						idempotencyKey : "user-${RUN_SUFFIX}",
						operation      : 'user.create',
						onError        : [policy: 'FAIL_FAST'],
						params         : [
							[name: 'count', value: 1],
							[name: 'baseName', value: USER_BASE_NAME],
							[
								name: 'groupIds',
								from: 'steps.createSite.items[0].groupId'
							],
							[
								name: 'siteRoleIds',
								from: 'steps.createRole.items[0].roleId'
							]
						]
					],
				[
					id             : 'createVocabulary',
					idempotencyKey : "vocab-${RUN_SUFFIX}",
					operation      : 'vocabulary.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: VOCAB_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						]
					]
				],
				[
					id             : 'createWebContent',
					idempotencyKey : "web-content-${RUN_SUFFIX}",
					operation      : 'webContent.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: WEB_CONTENT_BASE_NAME],
						[
							name: 'groupIds',
							from: 'steps.createSite.items[0].groupId'
						],
						[name: 'createContentsType', value: 0],
						[name: 'baseArticle', value: 'Workflow HTTP e2e body'],
						[name: 'folderId', value: 0]
					]
				],
				[
					id             : 'createDocument',
					idempotencyKey : "document-${RUN_SUFFIX}",
					operation      : 'document.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: DOC_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						],
						[name: 'folderId', value: 0]
					]
				],
				[
					id             : 'createCategory',
					idempotencyKey : "category-${RUN_SUFFIX}",
					operation      : 'category.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: CATEGORY_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						],
						[
							name: 'vocabularyId',
							from: 'steps.createVocabulary.items[0].vocabularyId'
						]
					]
				],
				[
					id             : 'createMBCategory',
					idempotencyKey : "mb-category-${RUN_SUFFIX}",
					operation      : 'mbCategory.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: MB_CATEGORY_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						],
						[name: 'description', value: 'Workflow HTTP e2e MB category']
					]
				],
				[
					id             : 'createMBThread',
					idempotencyKey : "mb-thread-${RUN_SUFFIX}",
					operation      : 'mbThread.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: MB_THREAD_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						],
						[
							name: 'categoryId',
							from: 'steps.createMBCategory.items[0].categoryId'
						],
						[name: 'body', value: 'Workflow HTTP e2e thread body'],
						[name: 'format', value: 'html']
					]
				],
				[
					id             : 'createLayout',
					idempotencyKey : "layout-${RUN_SUFFIX}",
					operation      : 'layout.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: PAGE_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						],
						[name: 'type', value: 'portlet'],
						[name: 'privateLayout', value: false],
						[name: 'hidden', value: false]
					]
				],
				[
					id             : 'createBlog',
					idempotencyKey : "blog-${RUN_SUFFIX}",
					operation      : 'blogs.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: BLOG_BASE_NAME],
						[
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId'
						],
						[name: 'content', value: '<p>Workflow HTTP e2e blog body</p>']
					]
				]
			]
		]
	}

}
