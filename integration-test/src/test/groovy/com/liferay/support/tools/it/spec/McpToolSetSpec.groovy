package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared

class McpToolSetSpec extends BaseLiferaySpec {

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long nonAdminUserId

	@Shared
	String nonAdminAuthorization

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so Basic Auth calls use the active credentials.
		loginAsAdmin(pw)

		String baseName = "nonadmin${System.currentTimeMillis()}"
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/operations/user.create', basicAuthHeader(),
			JsonOutput.toJson([count: 1, baseName: baseName]))
		assert response.status == 200 : "non-admin setup failed: ${response}"

		Map step = new JsonSlurper().parseText(response.body as String) as Map
		nonAdminUserId = step.result?.items?.getAt(0)?.userId as Long
		assert nonAdminUserId > 0 : "setup user ID missing: ${step}"
		assert step.result.success == true : "setup batch failed: ${step}"

		String email = step.result.items[0].emailAddress as String
		Map user = jsonwsGet(
			"user/get-user-by-email-address?companyId=${companyId}&emailAddress=${email}") as Map
		assert (user.emailAddress as String)?.equalsIgnoreCase("${baseName}1@liferay.com") :
			"JSONWS returned a different setup user: ${user}"
		assert (user.userId as Long) == nonAdminUserId
		nonAdminAuthorization = 'Basic ' + "${email}:test".getBytes('UTF-8').encodeBase64().toString()
	}

	def cleanupSpec() {
		try {
			if (nonAdminUserId != null) {
				jsonwsPost('user/delete-user', [userId: nonAdminUserId])
			}
		}
		finally {
			pw?.close()
		}
	}

	def 'ldf-workflow is listed as an MCP tool set'() {
		when:
		List names = headlessGet('/o/mcp-server/v1.0/tool-sets').items*.name

		then:
		assert names.contains('ldf-workflow') : "ldf-workflow missing from tool sets: ${names}"
	}

	def 'ldf-workflow exposes the coarse tools and one tool per operation'() {
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
			'executeWorkflow', 'createBlogsEntries', 'createCategories',
			'createCompanies', 'createDocuments', 'createLayouts',
			'createMBCategories', 'createMBReplies', 'createMBThreads',
			'createOrganizations', 'createRoles', 'createSites', 'createUsers',
			'createVocabularies', 'createWebContents'
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
		assert (document.paths as Map).keySet() == ([
			'/functions', '/schema', '/plan', '/execute',
			'/operations/blogs.create', '/operations/category.create',
			'/operations/company.create', '/operations/document.create',
			'/operations/layout.create', '/operations/mbCategory.create',
			'/operations/mbReply.create', '/operations/mbThread.create',
			'/operations/organization.create', '/operations/role.create',
			'/operations/site.create', '/operations/user.create',
			'/operations/vocabulary.create', '/operations/webContent.create'
		] as Set) :
			"unexpected paths: ${(document.paths as Map).keySet()}"
		assert document.paths['/operations/role.create'].post.summary == 'Create roles' :
			"unexpected role summary: ${document.paths['/operations/role.create']}"
		assert document.paths['/operations/role.create'].post.description ==
			'Runs role.create as a single step. Returns the step result {stepId, operation, status, result: {success, requested, count, skipped, items, error}, error}. HTTP 400 when a parameter is unknown or a required parameter is missing; HTTP 422 when the step fails, including invalid values such as a count out of range. HTTP 404 when the operation is unknown.' :
			"unexpected role description: ${document.paths['/operations/role.create']}"
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

	def 'execute rejects a non-admin user with 403 and creates nothing'() {
		given:
		assert nonAdminAuthorization : 'non-admin setup did not complete'
		String roleName = "mcp-nonadmin-execute-${System.currentTimeMillis()}"
		Map request = [
			schemaVersion: '1.0',
			steps: [[
				id: 'createRole', operation: 'role.create',
				idempotencyKey: "nonadmin-${System.nanoTime()}",
				params: [
					[name: 'count', value: 1],
					[name: 'baseName', value: roleName],
					[name: 'roleType', value: 'regular']
				]
			]]
		]

		when:
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/execute', nonAdminAuthorization,
			JsonOutput.toJson(request))

		then:
		assert response.status == 403 : "non-admin request was not rejected: ${response}"
		assert (response.body as String).contains('FORBIDDEN') :
			"authorization error missing: ${response}"

		when:
		List roleNames = (jsonwsGet(
			"role/get-roles/company-id/${companyId}/types/1") as List)*.name

		then:
		assert roleNames.contains('Administrator') :
			"role listing did not return regular roles: ${roleNames}"
		assert !roleNames.contains(roleName) : "non-admin request created role ${roleName}"
	}

	def 'createUsers creates users through MCP'() {
		when:
		Map result = _callTool(
			'postToolSetToolSetNameToolInvoke',
			[toolSetName: 'ldf-workflow', toolName: 'createUsers',
			 body: [body: [count: 3, baseName: 'mcpuser']]])

		then:
		assert result.isError == false : "createUsers failed: ${result}"

		when:
		Map response = new JsonSlurper().parseText(
			result.content[0].text as String) as Map

		then:
		assert response.status == 'SUCCEEDED' : "createUsers did not succeed: ${response}"
		assert response.result.success == true : "createUsers batch failed: ${response}"
		assert response.result.count == 3 : "unexpected created count: ${response}"
		assert response.result.requested == 3 : "unexpected requested count: ${response}"
		assert response.result.containsKey('skipped') : "skipped missing: ${response}"

		when:
		List users = (1..3).collect { i ->
			jsonwsGet("user/get-user-by-email-address?companyId=${companyId}&emailAddress=mcpuser${i}@liferay.com")
		}

		then:
		(1..3).each { i ->
			Map user = users[i - 1] as Map
			assert (user.emailAddress as String)?.equalsIgnoreCase("mcpuser${i}@liferay.com") :
				"JSONWS returned a different user: ${user}"
		}
	}

	def 'createUsers rejects an invalid baseName with isError'() {
		when:
		Map result = _callTool(
			'postToolSetToolSetNameToolInvoke',
			[toolSetName: 'ldf-workflow', toolName: 'createUsers',
			 body: [body: [count: 1, baseName: "O'Brien"]]])

		then:
		assert result.isError == true : "invalid baseName was accepted: ${result}"
		assert (result.content[0].text as String).contains('Status code: 422') :
			"unexpected failure status: ${result}"
	}

	def 'createUsers rejects count above the cap'() {
		when:
		Map result = _callTool(
			'postToolSetToolSetNameToolInvoke',
			[toolSetName: 'ldf-workflow', toolName: 'createUsers',
			 body: [body: [count: 1001, baseName: 'mcpcap']]])

		then:
		assert result.isError == true : "count above the cap was accepted: ${result}"
		assert (result.content[0].text as String).contains('Status code: 422') :
			"unexpected failure status: ${result}"
		assert (result.content[0].text as String).contains('1000') :
			"count cap missing from failure: ${result}"

		when:
		jsonwsGet("user/get-user-by-email-address?companyId=${companyId}&emailAddress=mcpcap1@liferay.com")

		then:
		IllegalStateException exception = thrown()
		assert exception.message.contains('returned HTTP 404') :
			"unexpected user lookup failure: ${exception.message}"

		and: 'the same lookup finds an existing user, so the 404 means absence'
		Map admin = jsonwsGet("user/get-user-by-email-address?companyId=${companyId}&emailAddress=test@liferay.com") as Map
		assert (admin?.emailAddress as String)?.equalsIgnoreCase('test@liferay.com') :
			"admin lookup failed: ${admin}"
	}

	def 'operations endpoint rejects a request without credentials and creates nothing'() {
		given:
		String roleName = "mcp-guest-operation-role-${System.currentTimeMillis()}"

		when:
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/operations/role.create', null,
			JsonOutput.toJson([count: 1, baseName: roleName, roleType: 'regular']))

		then:
		assert response.status == 401 : "unauthenticated operation was not rejected: ${response}"

		when:
		List roleNames = (jsonwsGet(
			"role/get-roles/company-id/${companyId}/types/1") as List)*.name

		then:
		assert roleNames.contains('Administrator') :
			"role listing did not return regular roles: ${roleNames}"
		assert !roleNames.contains(roleName) : "guest operation created role ${roleName}"
	}

	def 'operations endpoint rejects a non-admin user with 403 and creates nothing'() {
		given:
		assert nonAdminAuthorization : 'non-admin setup did not complete'
		String roleName = "mcp-nonadmin-operation-${System.currentTimeMillis()}"

		when:
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/operations/role.create', nonAdminAuthorization,
			JsonOutput.toJson([count: 1, baseName: roleName, roleType: 'regular']))

		then:
		assert response.status == 403 : "non-admin request was not rejected: ${response}"
		assert (response.body as String).contains('FORBIDDEN') :
			"authorization error missing: ${response}"

		when:
		List roleNames = (jsonwsGet(
			"role/get-roles/company-id/${companyId}/types/1") as List)*.name

		then:
		assert roleNames.contains('Administrator') :
			"role listing did not return regular roles: ${roleNames}"
		assert !roleNames.contains(roleName) : "non-admin request created role ${roleName}"
	}

	def 'operations endpoint answers 404 for an unknown operation'() {
		when:
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/operations/nope.create', basicAuthHeader(),
			JsonOutput.toJson([count: 1, baseName: 'unknown-role']))

		then:
		assert response.status == 404 : "unexpected unknown-operation status: ${response}"
		assert (response.body as String).contains('OPERATION_UNKNOWN') :
			"unknown-operation error missing: ${response}"
	}

	def 'operations endpoint answers 422 when count is 0'() {
		when:
		Map response = _rawRequest(
			'POST', '/o/ldf-workflow/operations/role.create', basicAuthHeader(),
			JsonOutput.toJson([count: 0, baseName: 'zero-role']))

		then:
		assert response.status == 422 : "unexpected zero-count status: ${response}"
		assert (response.body as String).contains('FAILED') :
			"failed step missing: ${response}"
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
