package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared

class McpServerSmokeSpec extends BaseLiferaySpec {

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

	def 'MCP endpoint answers initialize with server info'() {
		given:
		Map request = [
			jsonrpc: '2.0', id: 1, method: 'initialize',
			params: [
				protocolVersion: '2025-06-18', capabilities: [:],
				clientInfo: [name: 'ldf-it', version: '1.0']
			]
		]

		when:
		Map response = _mcpPost('/o/mcp', request)

		then:
		assert response.error == null : "initialize failed: ${response}"
		response.result.serverInfo != null
		response.result.capabilities.tools != null
	}

	def 'default profile lists the four meta tools'() {
		given:
		Map request = [jsonrpc: '2.0', id: 2, method: 'tools/list', params: [:]]

		when:
		Map response = _mcpPost('/o/mcp', request)

		then:
		assert response.error == null : "tools/list failed: ${response}"
		assert (response.result.tools*.name as Set).containsAll([
			'getToolSetsPage',
			'getToolSetToolSetNameToolSummariesPage',
			'getToolSetToolSetNameTool',
			'postToolSetToolSetNameToolInvoke'
		]) : "missing meta tools: ${response}"
	}

	def 'tool-sets REST lists Liferay headless applications'() {
		given:
		String path = '/o/mcp-server/v1.0/tool-sets'

		when:
		Map response = headlessGet(path)

		then:
		assert (response.items as List).size() > 0 : "no tool sets: ${response}"
		assert (response.items*.name).any {
			(it as String).startsWith('headless-admin-user')
		} : "no headless-admin-user tool set: ${response.items*.name}"
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
