package com.liferay.support.tools.it.util

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.RequestOptions

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.URLEncoder

class WorkflowHttpClient {

	WorkflowHttpClient(String baseUrl, Page page) {
		_baseUrl = baseUrl.endsWith('/') ? baseUrl[0..-2] : baseUrl
		_page = page
	}

	Map execute(Map<String, Object> requestBody) {
		return _post('/o/ldf-workflow/execute', requestBody)
	}

	Map plan(Map<String, Object> requestBody) {
		return _post('/o/ldf-workflow/plan', requestBody)
	}

	Map functions() {
		return _get('/o/ldf-workflow/functions')
	}

	private Map _post(String path, Map<String, Object> requestBody) {
		String authToken = _authToken()
		String url = "${_baseUrl}${path}"

		if (authToken) {
			url = "${url}?p_auth=${URLEncoder.encode(authToken, 'UTF-8')}"
		}

		def response = _page.request().post(
			url,
			RequestOptions.create(
			).setHeader(
				'Accept', 'application/json'
			).setHeader(
				'Content-Type', 'application/json'
			).setTimeout(
				60_000
			).setData(
				JsonOutput.toJson(requestBody ?: [:])))

		int status = response.status()
		String responseBody = response.text() ?: ''

		if (status >= 400) {
			throw new IllegalStateException(
				"POST ${path} returned HTTP ${status}: ${responseBody}")
		}

		if (!responseBody.trim()) {
			return [:]
		}

		return new JsonSlurper().parseText(responseBody) as Map
	}

	private Map _get(String path) {
		String authToken = _authToken()
		String url = "${_baseUrl}${path}"

		if (authToken) {
			url = "${url}?p_auth=${URLEncoder.encode(authToken, 'UTF-8')}"
		}

		def response = _page.request().get(
			url,
			RequestOptions.create(
			).setHeader(
				'Accept', 'application/json'
			).setTimeout(
				60_000
			))

		int status = response.status()
		String responseBody = response.text() ?: ''

		if (status >= 400) {
			throw new IllegalStateException(
				"GET ${path} returned HTTP ${status}: ${responseBody}")
		}

		if (!responseBody.trim()) {
			return [:]
		}

		return new JsonSlurper().parseText(responseBody) as Map
	}

	private String _authToken() {
		return _page.evaluate('() => Liferay.authToken') as String
	}

	private final String _baseUrl
	private final Page _page

}
