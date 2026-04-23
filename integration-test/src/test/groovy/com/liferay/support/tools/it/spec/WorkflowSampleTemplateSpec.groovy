package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.liferay.support.tools.it.util.WorkflowHttpClient

import groovy.json.JsonSlurper

import spock.lang.IgnoreIf
import spock.lang.Shared

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Executes the bundled sample workflow templates end-to-end. */
class WorkflowSampleTemplateSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		WorkflowSampleTemplateSpec)

	/**
	 * Millis-based suffix to prevent idempotencyKey and workflowId collisions
	 * across repeated test runs against the same container instance.
	 */
	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	@Shared
	PlaywrightLifecycle pw

	@Shared
	WorkflowHttpClient workflowHttpClient

	@Shared
	Long createdSiteGroupId

	@Shared
	Long createdPlid

	@Shared
	Long createdUserId

	@Shared
	Long createdOrganizationId

	@Shared
	Long createdVocabularyId

	@Shared
	Long createdCategoryId

	@Shared
	Long createdVocabularySiteGroupId

	@Shared
	Long createdRoleId

	@Shared
	Long createdMBSiteGroupId

	@Shared
	Long createdBlogWCSiteGroupId

	@Shared
	Long createdDocSiteGroupId

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		loginAsAdmin(pw)

		workflowHttpClient = new WorkflowHttpClient(liferay.baseUrl, pw.page)

		_waitForWorkflowOperations()
	}

	def cleanupSpec() {
		// Category must be deleted before its parent vocabulary.
		_safeCleanup('category', createdCategoryId) {
			headlessDelete(
				"/o/headless-admin-taxonomy/v1.0/taxonomy-categories" +
				"/${createdCategoryId}")
		}

		_safeCleanup('vocabulary', createdVocabularyId) {
			headlessDelete(
				"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" +
				"/${createdVocabularyId}")
		}

		_safeCleanup('vocabulary site group', createdVocabularySiteGroupId) {
			jsonwsPost('group/delete-group', ['groupId': createdVocabularySiteGroupId])
		}

		// createdPlid: layouts are deleted with the site; no explicit cleanup needed.

		_safeCleanup('organization', createdOrganizationId) {
			jsonwsPost(
				'organization/delete-organization',
				['organizationId': createdOrganizationId])
		}

		_safeCleanup('user', createdUserId) {
			jsonwsPost('user/delete-user', ['userId': createdUserId])
		}

		// If role/delete-role is unavailable in DXP 2026, the disposable container covers this.
		_safeCleanup('role', createdRoleId) {
			jsonwsPost('role/delete-role', ['roleId': createdRoleId])
		}

		_safeCleanup('message-boards site', createdMBSiteGroupId) {
			jsonwsPost('group/delete-group', ['groupId': createdMBSiteGroupId])
		}

		_safeCleanup('blogs/web-content site', createdBlogWCSiteGroupId) {
			jsonwsPost('group/delete-group', ['groupId': createdBlogWCSiteGroupId])
		}

		_safeCleanup('documents site', createdDocSiteGroupId) {
			jsonwsPost('group/delete-group', ['groupId': createdDocSiteGroupId])
		}

		_safeCleanup('site', createdSiteGroupId) {
			jsonwsPost('group/delete-group', ['groupId': createdSiteGroupId])
		}

		// Company: CompanyService is blacklisted from JSON-WS in DXP 2026
		// (json.service.invalid.class.names). Container is disposable, so no
		// explicit company cleanup is needed.

		workflowHttpClient = null

		try {
			pw?.close()
		}
		catch (Exception e) {
			log.warn('Playwright close failed: {}', e.message)
		}
	}

	def 'sample site-and-page template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('site-and-page.json')

		fixture.workflowId = "sample-site-and-page-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		// Suffix the site's baseName so repeated runs against the same
		// container, or co-existing specs that exercise the UI sample, do
		// not collide on site name uniqueness (DuplicateGroupException).
		Map createSiteStep = (fixture.steps as List<Map>).find {
			(it.id as String) == 'createSite'
		} as Map

		_setParamValue(
			createSiteStep.params as List<Map>, 'baseName',
			"sample-site-${RUN_SUFFIX}")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 2
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map siteStep = _stepById(steps, 'createSite')
		Map pageStep = _stepById(steps, 'createPage')

		Map siteResult = siteStep.result as Map
		Map pageResult = pageStep.result as Map

		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map pageItem = (pageResult.items as List<Map<String, Object>>).first()

		createdSiteGroupId = siteItem.groupId as Long
		createdPlid = pageItem.plid as Long

		then:
		_hasBatchResponseShape(siteResult)
		(siteResult.count as Number) == 1
		createdSiteGroupId > 0

		and:
		_hasBatchResponseShape(pageResult)
		(pageResult.count as Number) == 1
		createdPlid > 0

		and: 'site exists via JSONWS group lookup'
		Map createdSite = jsonwsGet(
			"group/get-group/group-id/${createdSiteGroupId}") as Map
		(createdSite.groupId as Long) == createdSiteGroupId
		(createdSite.nameCurrentValue as String).startsWith('sample-site')
	}

	def 'sample vocabulary-and-category template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('vocabulary-and-category.json')

		fixture.workflowId = "sample-vocabulary-and-category-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		// Suffix the site's baseName so repeated runs do not collide.
		// Vocabulary/category are site-scoped and inherit uniqueness from
		// the unique site created above, so they do not need suffixing.
		Map createSiteStep = (fixture.steps as List<Map>).find {
			(it.id as String) == 'createSite'
		} as Map

		_setParamValue(
			createSiteStep.params as List<Map>, 'baseName',
			"sample-vocabulary-site-${RUN_SUFFIX}")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 3
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map siteStep = _stepById(steps, 'createSite')
		Map vocabularyStep = _stepById(steps, 'createVocabulary')
		Map categoryStep = _stepById(steps, 'createCategory')

		Map siteResult = siteStep.result as Map
		Map vocabularyResult = vocabularyStep.result as Map
		Map categoryResult = categoryStep.result as Map

		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map vocabularyItem =
			(vocabularyResult.items as List<Map<String, Object>>).first()
		Map categoryItem =
			(categoryResult.items as List<Map<String, Object>>).first()

		createdVocabularySiteGroupId = siteItem.groupId as Long
		createdVocabularyId = vocabularyItem.vocabularyId as Long
		createdCategoryId = categoryItem.categoryId as Long

		Long vocabGroupId = vocabularyItem.groupId as Long

		then:
		_hasBatchResponseShape(siteResult)
		(siteResult.count as Number) == 1
		createdVocabularySiteGroupId > 0

		and:
		_hasBatchResponseShape(vocabularyResult)
		(vocabularyResult.count as Number) == 1
		createdVocabularyId > 0

		and:
		_hasBatchResponseShape(categoryResult)
		(categoryResult.count as Number) == 1
		createdCategoryId > 0
		(categoryItem.vocabularyId as Long) == createdVocabularyId

		and: 'vocabulary exists via headless taxonomy API'
		List vocabItems = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/sites/${vocabGroupId}" +
			'/taxonomy-vocabularies?pageSize=100').items as List
		vocabItems.any { (it.id as Long) == createdVocabularyId }

		and: 'category exists via headless taxonomy API'
		List catItems = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" +
			"/${createdVocabularyId}/taxonomy-categories?pageSize=100").items as List
		catItems.any { (it.id as Long) == createdCategoryId }
	}

	def 'sample role template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('role.json')

		fixture.workflowId = "sample-role-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		Map createRoleStep = (fixture.steps as List<Map>).first() as Map

		_setParamValue(
			createRoleStep.params as List<Map>, 'baseName',
			"sample-workflow-role-${RUN_SUFFIX}")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 1
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map roleStep = _stepById(steps, 'createRole')
		Map roleResult = roleStep.result as Map

		Map roleItem = (roleResult.items as List<Map<String, Object>>).first()

		createdRoleId = roleItem.roleId as Long

		then:
		_hasBatchResponseShape(roleResult)
		(roleResult.count as Number) == 1
		createdRoleId > 0
		(roleItem.name as String).startsWith("sample-workflow-role-${RUN_SUFFIX}")

		and: 'role type maps to REGULAR (1)'
		(roleItem.type as Integer) == 1

		and: 'role exists via JSONWS'
		Map createdRole = jsonwsGet(
			"role/get-role/company-id/${companyId}" +
			"/name/${URLEncoder.encode(roleItem.name as String, 'UTF-8')}") as Map
		(createdRole.roleId as Long) == createdRoleId
	}

	def 'sample documents template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('documents.json')

		fixture.workflowId = "sample-documents-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		Map createSiteStep = (fixture.steps as List<Map>).find {
			(it.id as String) == 'createSite'
		} as Map

		_setParamValue(
			createSiteStep.params as List<Map>, 'baseName',
			"sample-doc-site-${RUN_SUFFIX}")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 2
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map siteStep = _stepById(steps, 'createSite')
		Map documentStep = _stepById(steps, 'createDocument')

		Map siteResult = siteStep.result as Map
		Map documentResult = documentStep.result as Map

		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map documentItem =
			(documentResult.items as List<Map<String, Object>>).first()

		createdDocSiteGroupId = siteItem.groupId as Long

		then:
		_hasBatchResponseShape(siteResult)
		(siteResult.count as Number) == 1
		createdDocSiteGroupId > 0

		and:
		_hasBatchResponseShape(documentResult)
		(documentResult.count as Number) == 2
		(documentItem.fileEntryId as Long) > 0
		(documentItem.title as String).startsWith('sample-workflow-document')

		and: 'documents exist via JSONWS'
		List fileEntries = jsonwsGet(
			"dlapp/get-file-entries/repository-id/${createdDocSiteGroupId}" +
			'/folder-id/0') as List
		fileEntries.size() >= 2
	}

	def 'sample blogs-and-web-content template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('blogs-and-web-content.json')

		fixture.workflowId = "sample-blogs-and-web-content-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		Map createSiteStep = (fixture.steps as List<Map>).find {
			(it.id as String) == 'createSite'
		} as Map

		_setParamValue(
			createSiteStep.params as List<Map>, 'baseName',
			"sample-blog-wc-site-${RUN_SUFFIX}")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 3
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map siteStep = _stepById(steps, 'createSite')
		Map blogsStep = _stepById(steps, 'createBlogs')
		Map webContentStep = _stepById(steps, 'createWebContent')

		Map siteResult = siteStep.result as Map
		Map blogsResult = blogsStep.result as Map
		Map webContentResult = webContentStep.result as Map

		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map blogItem = (blogsResult.items as List<Map<String, Object>>).first()
		Map webContentItem =
			(webContentResult.items as List<Map<String, Object>>).first()

		createdBlogWCSiteGroupId = siteItem.groupId as Long

		then:
		_hasBatchResponseShape(siteResult)
		(siteResult.count as Number) == 1
		createdBlogWCSiteGroupId > 0

		and:
		_hasBatchResponseShape(blogsResult)
		(blogsResult.count as Number) == 1
		(blogItem.entryId as Long) > 0

		and:
		_hasBatchResponseShape(webContentResult)
		(webContentResult.count as Number) == 1
		(webContentItem.created as Number) == 1
		(webContentItem.failed as Number) == 0
		(webContentItem.groupId as Long) == createdBlogWCSiteGroupId

		and: 'blog entry exists via JSONWS'
		List blogEntries = jsonwsGet(
			'blogs.blogsentry/get-group-entries' +
			"?groupId=${createdBlogWCSiteGroupId}&status=0&max=100") as List
		blogEntries.any { (it.entryId as Long) == (blogItem.entryId as Long) }
	}

	def 'sample message-boards template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('message-boards.json')

		fixture.workflowId = "sample-message-boards-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		Map createSiteStep = (fixture.steps as List<Map>).find {
			(it.id as String) == 'createSite'
		} as Map

		_setParamValue(
			createSiteStep.params as List<Map>, 'baseName',
			"sample-mb-site-${RUN_SUFFIX}")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 4
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map siteStep = _stepById(steps, 'createSite')
		Map mbCategoryStep = _stepById(steps, 'createMBCategory')
		Map mbThreadStep = _stepById(steps, 'createMBThread')
		Map mbReplyStep = _stepById(steps, 'createMBReply')

		Map siteResult = siteStep.result as Map
		Map mbCategoryResult = mbCategoryStep.result as Map
		Map mbThreadResult = mbThreadStep.result as Map
		Map mbReplyResult = mbReplyStep.result as Map

		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map mbCategoryItem =
			(mbCategoryResult.items as List<Map<String, Object>>).first()
		Map mbThreadItem =
			(mbThreadResult.items as List<Map<String, Object>>).first()
		Map mbReplyItem =
			(mbReplyResult.items as List<Map<String, Object>>).first()

		createdMBSiteGroupId = siteItem.groupId as Long

		Long mbCategoryId = mbCategoryItem.categoryId as Long
		Long mbThreadId = mbThreadItem.threadId as Long

		// Message-board reply primary key: Liferay's MBMessage entity exposes
		// messageId as its primary key. Creators that wrap MBMessage
		// (including reply creators in this project) propagate it by that
		// name, matching the Liferay domain model. See MBMessage.getMessageId.
		Long mbReplyMessageId = mbReplyItem.messageId as Long

		then:
		_hasBatchResponseShape(siteResult)
		(siteResult.count as Number) == 1
		createdMBSiteGroupId > 0

		and:
		_hasBatchResponseShape(mbCategoryResult)
		(mbCategoryResult.count as Number) == 1
		mbCategoryId > 0
		(mbCategoryItem.groupId as Long) == createdMBSiteGroupId

		and:
		_hasBatchResponseShape(mbThreadResult)
		(mbThreadResult.count as Number) == 1
		mbThreadId > 0
		(mbThreadItem.groupId as Long) == createdMBSiteGroupId
		(mbThreadItem.categoryId as Long) == mbCategoryId

		and:
		_hasBatchResponseShape(mbReplyResult)
		(mbReplyResult.count as Number) == 1
		mbReplyMessageId > 0

		// MBReply adapter itemMapper exposes body, messageId, subject only
		// (see MBReplyCreateWorkflowOperationAdapter). threadId is not
		// surfaced on the item, so a reply-to-thread linkage assertion is
		// intentionally omitted here.

		and: 'thread exists under category via headless'
		List mbThreads = headlessGet(
			"/o/headless-delivery/v1.0/message-board-sections" +
			"/${mbCategoryId}/message-board-threads?pageSize=100").items as List
		mbThreads.any { (it.id as Long) == mbThreadId }
	}

	// Skipped in CI: company creation triggers site initializer, page-template
	// generation, and batch-engine imports that exceed the 60 s Playwright HTTP
	// timeout on shared runners. WorkflowHttpE2ECompanySpec.plan verifies that
	// company.create is registered. Run locally to exercise the full path.
	@IgnoreIf({ System.getenv('CI') == 'true' })
	def 'sample company-user-organization template executes end-to-end'() {
		given:
		Map fixture = _loadFixture('company-user-organization.json')

		fixture.workflowId = "sample-company-user-organization-${RUN_SUFFIX}"

		(fixture.steps as List<Map>).each { step ->
			step.idempotencyKey = "${step.idempotencyKey}-${RUN_SUFFIX}"
		}

		// Make company params unique per run so repeated executions against
		// the same container do not collide on the webId unique constraint.
		Map createCompanyStep = (fixture.steps as List<Map>).find {
			(it.id as String) == 'createCompany'
		} as Map

		List<Map> companyParams = createCompanyStep.params as List<Map>

		_setParamValue(
			companyParams, 'webId', "sample-workflow-company${RUN_SUFFIX}")
		_setParamValue(
			companyParams, 'virtualHostname',
			"sample-workflow-company${RUN_SUFFIX}.local")
		_setParamValue(
			companyParams, 'mx',
			"sample-workflow-company${RUN_SUFFIX}.local")

		when:
		Map response = workflowHttpClient.execute(fixture)

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps.size() == 3
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map companyStep = _stepById(steps, 'createCompany')
		Map userStep = _stepById(steps, 'createUser')
		Map organizationStep = _stepById(steps, 'createOrganization')

		Map companyResult = companyStep.result as Map
		Map userResult = userStep.result as Map
		Map organizationResult = organizationStep.result as Map

		Map companyItem = (companyResult.items as List<Map<String, Object>>).first()
		Map userItem = (userResult.items as List<Map<String, Object>>).first()
		Map organizationItem =
			(organizationResult.items as List<Map<String, Object>>).first()

		createdUserId = userItem.userId as Long
		createdOrganizationId = organizationItem.organizationId as Long

		then:
		_hasBatchResponseShape(companyResult)
		(companyResult.count as Number) == 1
		(companyItem.companyId as Long) > 0
		(companyItem.webId as String) == "sample-workflow-company${RUN_SUFFIX}"

		and:
		_hasBatchResponseShape(userResult)
		(userResult.count as Number) == 1
		createdUserId > 0

		and:
		_hasBatchResponseShape(organizationResult)
		(organizationResult.count as Number) == 1
		createdOrganizationId > 0
		(organizationItem.name as String).startsWith('sample-workflow-organization')

		// CompanyService is blacklisted from JSON-WS in DXP 2026
		// (json.service.invalid.class.names); verify via step result only.

		and: 'user exists via JSONWS'
		Map createdUser = jsonwsGet(
			"user/get-user-by-id/user-id/${createdUserId}") as Map
		(createdUser.userId as Long) == createdUserId
		(createdUser.screenName as String).startsWith('sample-workflow-user')

		and: 'organization exists via JSONWS'
		Map createdOrganization = jsonwsGet(
			"organization/get-organization" +
			"/organization-id/${createdOrganizationId}") as Map
		(createdOrganization.organizationId as Long) == createdOrganizationId
		(createdOrganization.name as String).startsWith('sample-workflow-organization')
	}

	private void _safeCleanup(String label, Long id, Closure action) {
		if (!id) {
			return
		}

		try {
			action()
		}
		catch (Exception e) {
			log.warn('Failed to clean up {} {}: {}', label, id, e.message)
		}
	}

	/** Returns a mutable LinkedHashMap; call sites mutate in place. */
	private Map _loadFixture(String filename) {
		String resourcePath = "/workflow-samples/${filename}"
		InputStream stream = getClass().getResourceAsStream(resourcePath)

		assert stream != null :
			"Fixture not found on test classpath: ${resourcePath}. " +
			"Ensure integration-test/src/test/resources is on testRuntimeClasspath."

		return stream.withCloseable { s ->
			new JsonSlurper().parse(s) as Map
		}
	}

	private void _waitForWorkflowOperations() {
		List<String> required = [
			'blogs.create',
			'category.create',
			'company.create',
			'document.create',
			'layout.create',
			'mbCategory.create',
			'mbReply.create',
			'mbThread.create',
			'organization.create',
			'role.create',
			'site.create',
			'user.create',
			'vocabulary.create',
			'webContent.create'
		]

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

				if (operations.containsAll(required)) {
					return
				}

				lastException = new IllegalStateException(
					"Missing workflow operations: " +
						(required - operations).join(', '))
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

	private static boolean _hasBatchResponseShape(Map result) {
		return ['success', 'count', 'requested', 'skipped', 'items'].every {
			result.containsKey(it)
		}
	}

	private static Map<String, Object> _stepById(
		List<Map<String, Object>> steps, String stepId) {

		Map<String, Object> step = steps.find {
			(it.stepId as String) == stepId
		} as Map<String, Object>

		assert step != null : "Step '${stepId}' not found in execution result"

		return step
	}

	private static void _setParamValue(
		List<Map> params, String paramName, Object value) {

		Map param = params.find { (it.name as String) == paramName } as Map

		assert param != null : "Fixture is missing the '${paramName}' param"

		param.put('value', value)
	}

}
