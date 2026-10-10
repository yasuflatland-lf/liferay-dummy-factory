package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.JsonwsSetupHelper
import com.liferay.support.tools.it.util.LdfResourceClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.liferay.support.tools.it.util.WorkflowHttpClient

import spock.lang.Shared

class BatchCountCapSpec extends BaseLiferaySpec {

	@Shared
	private JsonwsSetupHelper _jsonws

	@Shared
	private LdfResourceClient _ldf

	@Shared
	private PlaywrightLifecycle _pw

	@Shared
	private WorkflowHttpClient _workflowHttpClient

	def setupSpec() {
		ensureBundleActive()

		_ldf = new LdfResourceClient(liferay.baseUrl)
		_ldf.login()

		_jsonws = new JsonwsSetupHelper(liferay.baseUrl)

		_pw = new PlaywrightLifecycle()
		loginAsAdmin(_pw)
		_workflowHttpClient = new WorkflowHttpClient(liferay.baseUrl, _pw.page)
	}

	def cleanupSpec() {
		_jsonws?.cleanupAll()
		_ldf?.close()
		_pw?.close()
	}

	def 'portlet resource command rejects count above 1000'() {
		given:
		Map request = [count: 1_001, baseName: 'capuser']

		when:
		Map response = _ldf.createUser(request)

		then:
		assert response.success == false : "expected rejection: ${response}"
		assert (response.error as String).contains('1000') : "unexpected error: ${response}"

		and:
		_assertTestUserExists()
		assert _capUserDoesNotExist() : "unexpected capuser1 after request: ${response}"
	}

	def 'web content resource command rejects count times sites above 1000'() {
		given:
		long siteAId = _jsonws.createSite("CapWcmA-${System.nanoTime()}").groupId as long
		long siteBId = _jsonws.createSite("CapWcmB-${System.nanoTime()}").groupId as long

		when:
		Map response = _ldf.createWebContent([
			count             : 600,
			baseName          : 'CapArticle',
			groupIds          : [siteAId, siteBId],
			createContentsType: '0',
			baseArticle       : 'Sample body',
			folderId          : 0
		])

		then:
		assert response.success == false : "expected rejection: ${response}"
		assert (response.error as String).contains('1000') : "unexpected error: ${response}"

		and:
		assert _articleCount(siteAId) == 0 : "articles created in site A: ${response}"
		assert _articleCount(siteBId) == 0 : "articles created in site B: ${response}"
	}

	def 'workflow execute rejects a step with count above 1000'() {
		given:
		String runSuffix = String.valueOf(System.currentTimeMillis())
		Map request = [
			schemaVersion: '1.0',
			workflowId   : "batch-count-cap-${runSuffix}",
			input        : [:],
			steps        : [
				[
					id            : 'createUser',
					idempotencyKey: "cap-user-${runSuffix}",
					operation     : 'user.create',
					onError       : [policy: 'FAIL_FAST'],
					params        : [
						[name: 'count', value: 1_001],
						[name: 'baseName', value: 'capuser']
					]
				]
			]
		]

		when:
		Map response = _workflowHttpClient.execute(request)

		then:
		assert response.execution instanceof Map : "expected execution: ${response}"
		Map execution = response.execution as Map
		assert execution.status == 'FAILED' : "expected failed execution: ${response}"
		assert execution.steps instanceof List : "expected steps: ${response}"
		Map failedStep = (execution.steps as List).find {
			it.stepId == 'createUser'
		} as Map
		assert failedStep?.status == 'FAILED' : "expected failed user step: ${response}"
		assert failedStep.error?.message != null : "expected step error: ${response}"
		assert (failedStep.error.message as String).contains('1000') : "unexpected step error: ${response}"

		and:
		_assertTestUserExists()
		assert _capUserDoesNotExist() : "unexpected capuser1 after workflow: ${response}"
	}

	private int _articleCount(long groupId) {
		return jsonwsGet(
			"journal.journalarticle/get-articles-count/group-id/${groupId}/folder-id/0") as int
	}

	private void _assertTestUserExists() {
		Object user = jsonwsGet(
			"user/get-user-by-screen-name?companyId=${companyId}&screenName=test")

		assert (user instanceof Map) && !user.containsKey('exception') : "test user lookup failed: ${user}"
		assert user.screenName == 'test' : "unexpected test user: ${user}"
	}

	private boolean _capUserDoesNotExist() {
		try {
			jsonwsGet(
				"user/get-user-by-screen-name?companyId=${companyId}&screenName=capuser1")

			return false
		}
		catch (IllegalStateException exception) {
			if (exception.message.contains('HTTP 404') &&
				exception.message.contains('NoSuchUserException')) {

				return true
			}

			throw exception
		}
	}

}
