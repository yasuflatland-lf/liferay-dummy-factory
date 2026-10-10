package com.liferay.support.tools.it.util

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets

/** Lives in it/util/ because it is also a developer CLI run through the setupMcpProfile JavaExec task, not just a test helper. */
class McpProfileProvisioner {

	static final String PROFILE_ERC = 'LDF_MCP_PROFILE'
	static final String PROFILE_NAME = 'ldf'
	static final String TOOL_SET_NAME = 'ldf-workflow'

	McpProfileProvisioner(String baseUrl, String authorizationHeader, String expectedEmail) {
		_baseUrl = baseUrl.endsWith('/') ? baseUrl[0..-2] : baseUrl
		_authorizationHeader = authorizationHeader
		_expectedEmail = expectedEmail
	}

	McpProfileProvisioner(String baseUrl, String authorizationHeader) {
		this(baseUrl, authorizationHeader,
			new String(Base64.decoder.decode(authorizationHeader.substring(6)),
				StandardCharsets.UTF_8).split(':', 2)[0])
	}

	/** Creates or updates the ldf profile; returns the sorted list of pinned tool names. */
	List<String> provision() {
		Map account = new JsonSlurper().parseText(_request(
			'GET', '/o/headless-admin-user/v1.0/my-user-account').body as String) as Map

		if (!(account.emailAddress as String)?.equalsIgnoreCase(_expectedEmail)) {
			throw new IllegalStateException(
				"Authenticated as ${account.emailAddress}, expected ${_expectedEmail}; check credentials")
		}

		Map discovery = _mcpPost('/o/mcp', [
			jsonrpc: '2.0', id: 1, method: 'tools/call',
			params: [name: 'getToolSetToolSetNameToolSummariesPage',
				arguments: [toolSetName: TOOL_SET_NAME]]
		])
		Map page = new JsonSlurper().parseText(
			discovery.result.content[0].text as String) as Map
		List<String> tools = page.items*.name as List<String>

		if (!tools) {
			throw new IllegalStateException("No tools discovered for ${TOOL_SET_NAME}: ${page}")
		}

		_request('PUT',
			"/o/mcp/server-profiles/by-external-reference-code/${PROFILE_ERC}",
			[name: PROFILE_NAME, description: _DESCRIPTION,
				tools: tools.sort(false).collect { "${TOOL_SET_NAME} ${it}" }.join('\n')])

		Map listing = _mcpPost("/o/mcp/${PROFILE_NAME}", [
			jsonrpc: '2.0', id: 1, method: 'tools/list', params: [:]
		])
		Set listedTools = listing.result.tools*.name as Set
		Set discoveredTools = tools as Set

		if (listedTools != discoveredTools) {
			throw new IllegalStateException(
				"Profile tools ${listedTools} differ from discovered tools ${discoveredTools}")
		}

		return tools.sort(false)
	}

	static void main(String[] args) {
		try {
			String baseUrl = System.getProperty('ldf.baseUrl', 'http://localhost:8080')
			String user = System.getProperty('ldf.user', 'test@liferay.com')
			String password = System.getProperty('ldf.password', 'test')
			String credentials = "${user}:${password}"
			String header = "Basic ${credentials.getBytes(StandardCharsets.UTF_8).encodeBase64()}"
			def provisioner = new McpProfileProvisioner(baseUrl, header, user)

			provisioner.provision().each { println it }
			println "MCP profile 'ldf' ready at ${baseUrl.replaceFirst(/\/$/, '')}/o/mcp/ldf"
		}
		catch (Exception e) {
			System.err.println(e.message)
			System.exit(1)
		}
	}

	private Map _mcpPost(String path, Map jsonRpcBody) {
		Map response = _request('POST', path, jsonRpcBody, 'application/json, text/event-stream')
		String body = response.body as String

		if ((response.contentType as String)?.startsWith('text/event-stream')) {
			body = body.split(/\r?\n\r?\n/).collect { event ->
				event.readLines().findAll { it.startsWith('data:') }
					.collect { it.substring(5).replaceFirst(/^ /, '') }.join('\n')
			}.findAll { it }.last()
		}

		Map result = new JsonSlurper().parseText(body) as Map

		if (result.error != null || result.result?.isError == true) {
			throw new IllegalStateException("POST ${path} returned an MCP error: ${result}")
		}

		return result
	}

	private Map _request(
		String method, String path, Map jsonBody = null, String accept = 'application/json') {

		def conn = new URL("${_baseUrl}${path}").openConnection() as HttpURLConnection

		try {
			conn.requestMethod = method
			conn.instanceFollowRedirects = false
			conn.setRequestProperty('Authorization', _authorizationHeader)
			conn.setRequestProperty('Accept', accept)
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.connectTimeout = 10_000
			conn.readTimeout = 60_000

			if (jsonBody != null) {
				conn.setRequestProperty('Content-Type', 'application/json')
				conn.doOutput = true
				conn.outputStream.withWriter('UTF-8') { writer ->
					writer.write(JsonOutput.toJson(jsonBody))
				}
			}

			int status = conn.responseCode
			String body = (status < 400)
				? (conn.inputStream?.getText('UTF-8') ?: '')
				: (conn.errorStream?.getText('UTF-8') ?: '')

			if (status < 200 || status >= 300) {
				throw new IllegalStateException(
					"${method} ${path} returned HTTP ${status}: ${body}")
			}

			return [body: body, contentType: conn.contentType]
		}
		finally {
			conn.disconnect()
		}
	}

	private static final String _DESCRIPTION =
		'Liferay Dummy Factory data-creation tools. Managed by ./gradlew :integration-test:setupMcpProfile; manual edits are overwritten.'

	private final String _baseUrl
	private final String _authorizationHeader
	private final String _expectedEmail

}
