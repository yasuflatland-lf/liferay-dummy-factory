package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.McpProfileProvisioner
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class McpProfileSpec extends BaseLiferaySpec {

	@Shared
	PlaywrightLifecycle pw

	@Shared
	McpProfileProvisioner provisioner

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so Basic Auth calls use the active credentials.
		loginAsAdmin(pw)
		provisioner = new McpProfileProvisioner(
			liferay.baseUrl, basicAuthHeader(), LiferayContainer.DEFAULT_ADMIN_EMAIL)
	}

	def cleanupSpec() {
		pw?.close()
		// The ldf profile is kept because the next run re-provisions it idempotently; the site has no JSONWS delete path.
	}

	def 'provisioning creates an ldf profile exposing every ldf-workflow tool'() {
		when:
		List<String> tools = provisioner.provision()

		then:
		assert (tools as Set) == _EXPECTED_TOOLS : "unexpected provisioned tools: ${tools}"
		assert tools == tools.sort(false) : "provisioned tools are not sorted: ${tools}"

		when:
		Map response = _mcpPost('/o/mcp/ldf', [
			jsonrpc: '2.0', id: 1, method: 'tools/list', params: [:]
		])

		then:
		assert response.error == null : "tools/list JSON-RPC error: ${response}"
		assert (response.result.tools*.name as Set) == _EXPECTED_TOOLS :
			"unexpected profile tools: ${response}"
	}

	def 'provisioning is idempotent'() {
		when:
		List<String> tools = provisioner.provision()

		then:
		assert (tools as Set) == _EXPECTED_TOOLS : "unexpected provisioned tools: ${tools}"

		when:
		Map response = _mcpPost('/o/mcp/ldf', [
			jsonrpc: '2.0', id: 1, method: 'tools/list', params: [:]
		])

		then:
		assert response.error == null : "tools/list JSON-RPC error: ${response}"
		assert response.result.tools.size() == 18 : "unexpected profile tool count: ${response}"
		assert (response.result.tools*.name as Set) == _EXPECTED_TOOLS :
			"unexpected profile tools: ${response}"
	}

	def 'createSites on the ldf profile creates a site'() {
		when:
		Map response = _mcpPost('/o/mcp/ldf', [
			jsonrpc: '2.0', id: 1, method: 'tools/call',
			params: [name: 'createSites',
				arguments: [body: [count: 1, baseName: 'mcp-profile-site']]]
		])

		then:
		assert response.error == null : "createSites JSON-RPC error: ${response}"
		assert response.result.isError == false : "createSites returned an error: ${response}"

		when:
		Map step = new JsonSlurper().parseText(
			response.result.content[0].text as String) as Map

		then:
		assert step.status == 'SUCCEEDED' : "createSites did not succeed: ${step}"
		assert step.result.success == true : "createSites batch failed: ${step}"
		assert step.result.count == 1 : "unexpected created count: ${step}"
		assert step.result.containsKey('skipped') : "skipped missing: ${step}"

		when:
		long groupId = step.result.items[0].groupId as long
		def group = jsonwsGet("group/get-group?groupId=${groupId}")

		then:
		assert (group.groupId as long) == groupId : "JSONWS returned a different group: ${group}"
	}

	private Map _mcpPost(String path, Map<String, Object> jsonRpcBody) {
		def conn = new URL(absoluteUrl(path)).openConnection() as HttpURLConnection

		try {
			conn.requestMethod = 'POST'
			conn.setRequestProperty('Authorization', basicAuthHeader())
			conn.setRequestProperty('Content-Type', 'application/json')
			conn.setRequestProperty('Accept', 'application/json, text/event-stream')
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.connectTimeout = 10_000
			conn.readTimeout = 60_000
			conn.doOutput = true
			conn.outputStream.withWriter('UTF-8') { writer ->
				writer.write(JsonOutput.toJson(jsonRpcBody))
			}

			int status = conn.responseCode
			String body = (status < 400)
				? (conn.inputStream?.getText('UTF-8') ?: '')
				: (conn.errorStream?.getText('UTF-8') ?: '')

			if (status >= 400) {
				throw new IllegalStateException(
					"POST ${path} returned HTTP ${status}: ${body}")
			}

			if (conn.contentType?.startsWith('text/event-stream')) {
				body = body.split(/\r?\n\r?\n/).collect { event ->
					event.readLines().findAll { it.startsWith('data:') }
						.collect { it.substring(5).replaceFirst(/^ /, '') }.join('\n')
				}.findAll { it }.last()
			}

			return new JsonSlurper().parseText(body) as Map
		}
		finally {
			conn.disconnect()
		}
	}

	private static final Set<String> _EXPECTED_TOOLS = [
		'getWorkflowFunctions', 'getWorkflowSchema', 'planWorkflow',
		'executeWorkflow', 'createBlogsEntries', 'createCategories',
		'createCompanies', 'createDocuments', 'createLayouts',
		'createMBCategories', 'createMBReplies', 'createMBThreads',
		'createOrganizations', 'createRoles', 'createSites', 'createUsers',
		'createVocabularies', 'createWebContents'
	] as Set

}
