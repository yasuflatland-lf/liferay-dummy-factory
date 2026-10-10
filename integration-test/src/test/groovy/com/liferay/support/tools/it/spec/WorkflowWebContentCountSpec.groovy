package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class WorkflowWebContentCountSpec extends BaseLiferaySpec {

	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long groupId

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()
		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		// group/delete-group is unavailable through JSONWS; the container is disposable.
		pw?.close()
	}

	def 'execute creates more web contents than target sites'() {
		given:
		Map request = [
			schemaVersion: '1.0',
			workflowId: "workflow-web-content-count-${RUN_SUFFIX}",
			steps: [
				[
					id: 'createSite', operation: 'site.create',
					idempotencyKey: "site-web-content-count-${RUN_SUFFIX}",
					params: [
						[name: 'count', value: 1],
						[name: 'baseName', value: "WFWebContentSite${RUN_SUFFIX}"],
						[name: 'membershipType', value: 'open'],
						[name: 'manualMembership', value: true]
					]
				],
				[
					id: 'createWebContent', operation: 'webContent.create',
					idempotencyKey: "web-content-count-${RUN_SUFFIX}",
					params: [
						[name: 'count', value: 3],
						[name: 'baseName', value: "WFWebContentArticle${RUN_SUFFIX}"],
						[name: 'groupIds', from: 'steps.createSite.items[0].groupId'],
						[name: 'createContentsType', value: 0],
						[name: 'baseArticle', value: 'Workflow HTTP e2e body'],
						[name: 'folderId', value: 0]
					]
				]
			]
		]

		when:
		Map response = _postJson('/o/ldf-workflow/execute', request)

		then:
		assert response.status == 200 : "${response}"

		when:
		Map payload = new JsonSlurper().parseText(response.body as String) as Map

		then:
		assert payload.errors == [] : "${payload}"
		assert payload.execution?.status == 'SUCCEEDED' : "${payload}"
		assert payload.execution.steps*.stepId == ['createSite', 'createWebContent'] :
			"${payload}"
		assert payload.execution.steps.every { it.status == 'SUCCEEDED' } : "${payload}"
		assert payload.execution.steps[0].result.items.size() == 1 : "${payload}"

		when:
		groupId = payload.execution.steps[0].result.items[0].groupId as Long
		Map result = payload.execution.steps[1].result as Map

		then:
		assert groupId > 0 : "${payload}"
		_assertOnePerSiteItemSuccess(payload, result, 3)

		when:
		int articleCount = _articleCount()

		then:
		assert articleCount == 3 : "article count ${articleCount}; ${payload}"
	}

	def 'operations endpoint creates more web contents than target sites'() {
		given:
		assert groupId != null && groupId > 0 :
			'prior feature method did not populate groupId; @Stepwise ordering broken'
		Map parameters = [
			count: 2, groupIds: [groupId],
			baseName: "WFWebContentOperationArticle${RUN_SUFFIX}",
			createContentsType: 0, baseArticle: 'Workflow HTTP e2e body', folderId: 0
		]

		when:
		Map response = _postJson(
			'/o/ldf-workflow/operations/webContent.create', parameters)

		then:
		assert response.status == 200 : "${response}"

		when:
		Map payload = new JsonSlurper().parseText(response.body as String) as Map
		Map result = payload.result as Map

		then:
		assert payload.status == 'SUCCEEDED' : "${payload}"
		_assertOnePerSiteItemSuccess(payload, result, 2)

		when:
		int articleCount = _articleCount()

		then:
		assert articleCount == 5 : "article count ${articleCount}; ${payload}"
	}

	def 'operations endpoint rejects missing template input before web content creation'() {
		given:
		assert groupId != null && groupId > 0 :
			'prior feature method did not populate groupId; @Stepwise ordering broken'
		Map parameters = [
			count: 2, groupIds: [groupId],
			baseName: "WFWebContentFailedArticle${RUN_SUFFIX}",
			createContentsType: 2, ddmStructureId: 999_999_999,
			ddmTemplateId: 999_999_999, folderId: 0
		]

		when:
		Map response = _postJson(
			'/o/ldf-workflow/operations/webContent.create', parameters)

		then:
		assert response.status == 422 : "${response}"

		when:
		Map payload = new JsonSlurper().parseText(response.body as String) as Map

		then:
		assert payload.status == 'FAILED' : "${payload}"
		assert payload.error?.message?.contains('ddmStructureId 999999999 does not exist') :
			"${payload}"

		when:
		int articleCount = _articleCount()

		then:
		assert articleCount == 5 : "article count ${articleCount}; ${payload}"
	}

	private void _assertOnePerSiteItemSuccess(Map payload, Map result, int count) {

		assert result != null : "${payload}"
		assert ['success', 'requested', 'count', 'skipped', 'items', 'error'].every {
			result.containsKey(it)
		} : "${payload}"
		assert result.success instanceof Boolean : "${payload}"
		assert ['requested', 'count', 'skipped'].every {
			result[it] instanceof Number
		} : "${payload}"
		assert result.items instanceof List : "${payload}"
		assert result.success == true : "${payload}"
		assert result.requested == count : "${payload}"
		assert result.count == count : "${payload}"
		assert result.skipped == 0 : "${payload}"
		assert result.error == null : "${payload}"
		assert result.items.size() == 1 : "${payload}"
		assert result.items[0].groupId == groupId : "${payload}"
		assert result.items[0].created == count : "${payload}"
		assert result.items[0].failed == 0 : "${payload}"
		assert result.items[0].siteName instanceof String : "${payload}"
		assert !result.items[0].containsKey('error') : "${payload}"
	}

	private int _articleCount() {
		return jsonwsGet(
			"journal.journalarticle/get-articles-count/group-id/${groupId}/folder-id/0") as int
	}

	private Map _postJson(String path, Map body) {
		def conn = new URL(absoluteUrl(path)).openConnection() as HttpURLConnection

		try {
			conn.requestMethod = 'POST'
			conn.instanceFollowRedirects = false
			conn.setRequestProperty('Authorization', basicAuthHeader())
			conn.setRequestProperty('Accept', 'application/json')
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.setRequestProperty('Content-Type', 'application/json')
			conn.connectTimeout = 10_000
			conn.readTimeout = 60_000
			conn.doOutput = true
			conn.outputStream.withWriter('UTF-8') { writer ->
				writer.write(JsonOutput.toJson(body))
			}

			int status = conn.responseCode
			String responseBody = (status < 400)
				? (conn.inputStream?.getText('UTF-8') ?: '')
				: (conn.errorStream?.getText('UTF-8') ?: '')

			return [status: status, body: responseBody]
		}
		finally {
			conn.disconnect()
		}
	}

}
