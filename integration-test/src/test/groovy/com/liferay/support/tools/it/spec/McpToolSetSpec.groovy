package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared

class McpToolSetSpec extends BaseLiferaySpec {

	@Shared
	PlaywrightLifecycle pw

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so Basic Auth calls use the active credentials.
		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		pw?.close()
	}

	def 'ldf-workflow is listed as an MCP tool set'() {
		when:
		List names = headlessGet('/o/mcp-server/v1.0/tool-sets').items*.name

		then:
		assert names.contains('ldf-workflow') : "ldf-workflow missing from tool sets: ${names}"
	}

	def 'ldf-workflow exposes exactly the four coarse tools'() {
		when:
		Map result = _callTool(
			'getToolSetToolSetNameToolSummariesPage',
			[toolSetName: 'ldf-workflow'])

		then:
		assert result.isError == false : "tool returned an error: ${result}"

		when:
		Map page = new JsonSlurper().parseText(
			result.content[0].text as String) as Map

		then:
		assert (page.items*.name as Set) == ([
			'getWorkflowFunctions', 'getWorkflowSchema', 'planWorkflow',
			'executeWorkflow'
		] as Set) : "unexpected workflow tools: ${page.items*.name}"
	}

	def 'getWorkflowFunctions is invocable through the default profile'() {
		when:
		Map result = _callTool(
			'postToolSetToolSetNameToolInvoke',
			[
				toolSetName: 'ldf-workflow', toolName: 'getWorkflowFunctions',
				body: [:]
			])

		then:
		assert result.isError == false : "tool returned an error: ${result}"

		when:
		Map catalog = new JsonSlurper().parseText(
			result.content[0].text as String) as Map

		then:
		assert (catalog.functions*.operation).contains('user.create') :
			"user.create missing from functions: ${catalog.functions*.operation}"
	}

	def 'planWorkflow validates a request through MCP without creating data'() {
		given:
		Map request = [
			schemaVersion: '1.0',
			steps: [[
				id: 'createSite', operation: 'site.create',
				idempotencyKey: 'mcp-plan-1',
				params: [
					[name: 'count', value: 1],
					[name: 'baseName', value: 'mcp-plan-site']
				]
			]]
		]

		when:
		Map result = _callTool(
			'postToolSetToolSetNameToolInvoke',
			[
				toolSetName: 'ldf-workflow', toolName: 'planWorkflow',
				body: [body: request]
			])

		then:
		assert result.isError == false : "tool returned an error: ${result}"

		when:
		Map response = new JsonSlurper().parseText(
			result.content[0].text as String) as Map

		then:
		assert response.errors == [] : "plan returned validation errors: ${response}"
		assert response.plan != null : "plan missing from response: ${response}"
	}

	def 'openapi.json is served as an OpenAPI 3.1 document'() {
		when:
		Map document = headlessGet('/o/ldf-workflow/openapi.json')

		then:
		assert document.openapi == '3.1.0' : "unexpected openapi version: ${document.openapi}"
		assert (document.paths as Map).keySet() == (['/functions', '/schema', '/plan', '/execute'] as Set) :
			"unexpected paths: ${(document.paths as Map).keySet()}"
	}

	def 'execute rejects a request without credentials and creates nothing'() {
		given:
		String roleName = "mcp-guest-role-${System.currentTimeMillis()}"
		Map request = [
			schemaVersion: '1.0',
			steps: [[
				id: 'createRole', operation: 'role.create',
				idempotencyKey: 'mcp-guest-1',
				params: [
					[name: 'count', value: 1],
					[name: 'baseName', value: roleName],
					[name: 'roleType', value: 'regular']
				]
			]]
		]

		when:
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/execute', null, JsonOutput.toJson(request))

		then:
		assert response.status == 401 : "unauthenticated execute was not rejected: ${response}"

		when:
		List roleNames = (jsonwsGet(
			"role/get-roles/company-id/${companyId}/types/1") as List)*.name

		then:
		assert roleNames.contains('Administrator') :
			"role listing did not return regular roles: ${roleNames}"
		assert !roleNames.contains(roleName) : "guest execute created role ${roleName}"
	}

	def 'a wrong password on ldf-workflow is answered with HTTP 401'() {
		when:
		Map response = _rawRequest(
			'GET', '/o/ldf-workflow/functions',
			'Basic ' + 'test@liferay.com:wrong-password'.bytes.encodeBase64().toString(),
			null)

		then:
		assert response.status == 401 : "wrong password was not rejected: ${response}"
	}

	private Map _rawRequest(
		String method, String path, String authorization, String jsonBody) {

		def conn = new URL(absoluteUrl(path)).openConnection() as HttpURLConnection

		try {
			conn.requestMethod = method
			conn.instanceFollowRedirects = false

			if (authorization != null) {
				conn.setRequestProperty('Authorization', authorization)
			}

			conn.setRequestProperty('Accept', 'application/json')
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.connectTimeout = 10_000
			conn.readTimeout = 60_000

			if (jsonBody != null) {
				conn.setRequestProperty('Content-Type', 'application/json')
				conn.doOutput = true
				conn.outputStream.withWriter('UTF-8') { writer ->
					writer.write(jsonBody)
				}
			}

			int status = conn.responseCode
			String body = (status < 400)
				? (conn.inputStream?.getText('UTF-8') ?: '')
				: (conn.errorStream?.getText('UTF-8') ?: '')

			return [status: status, body: body]
		}
		finally {
			conn.disconnect()
		}
	}

	private Map _callTool(String name, Map<String, Object> arguments) {
		Map response = _mcpPost('/o/mcp', [
			jsonrpc: '2.0', id: 1, method: 'tools/call',
			params: [name: name, arguments: arguments]
		])
		assert response.error == null : "tools/call ${name} JSON-RPC error: ${response}"
		return response.result as Map
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

}
