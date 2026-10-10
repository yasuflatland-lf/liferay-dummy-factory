package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared

class WorkflowCompanyScopeSpec extends BaseLiferaySpec {

	@Shared
	PlaywrightLifecycle pw

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		// CompanyService is blacklisted from JSONWS; no remote company delete path exists.
		pw?.close()
	}

	def 'omniadmin cannot create blogs in another virtual instance'() {
		given:
		String suffix = "scope${System.currentTimeMillis()}"
		Map caller = jsonwsGet('user/get-current-user') as Map
		assert caller.emailAddress.equalsIgnoreCase(LiferayContainer.DEFAULT_ADMIN_EMAIL)

		when:
		Map companyResponse = _operation('company.create', [
			count: 1, webId: suffix, virtualHostname: "${suffix}.example.com",
			mx: "${suffix}.example.com"
		])

		then:
		assert companyResponse.status == 200 : "${companyResponse}"
		assert companyResponse.payload.result?.error == null : "${companyResponse}"
		assert companyResponse.payload.result?.success == true : "${companyResponse}"
		assert companyResponse.payload.result.items[0].webId == suffix : "${companyResponse}"

		when:
		long companyId = companyResponse.payload.result.items[0].companyId as long
		Map group = jsonwsGet("group/get-company-group/company-id/${companyId}") as Map
		long groupId = group.groupId as long

		then:
		assert group.companyId as long == companyId : "${group}"
		assert companyId != (caller.companyId as long)
		assert groupId > 0
		assert _blogs(groupId).isEmpty()

		when:
		Map response = _operation('blogs.create', [
			count: 1, baseName: "CrossCompany${suffix}", groupId: groupId,
			content: 'Must never be created'
		])

		then:
		assert response.status == 422 : "${response}"
		assert response.payload.status == 'FAILED' : "${response}"
		assert response.payload.error?.message?.contains(
			"groupId ${groupId} does not belong to the caller's company") : "${response}"
		assert _blogs(groupId).isEmpty()
	}

	private List _blogs(long groupId) {
		return jsonwsGet('blogs.blogsentry/get-group-entries' +
			"?groupId=${groupId}&status=-1&max=100") as List
	}

	private Map _operation(String operation, Map body) {
		def conn = new URL(absoluteUrl(
			"/o/ldf-workflow/operations/${operation}")).openConnection() as HttpURLConnection

		try {
			conn.requestMethod = 'POST'
			conn.instanceFollowRedirects = false
			conn.setRequestProperty('Authorization', basicAuthHeader())
			conn.setRequestProperty('Accept', 'application/json')
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.setRequestProperty('Content-Type', 'application/json')
			conn.connectTimeout = 10_000
			// Company creation runs the site initializers synchronously.
			conn.readTimeout = 120_000
			conn.doOutput = true
			conn.outputStream.withWriter('UTF-8') { it.write(JsonOutput.toJson(body)) }
			int status = conn.responseCode
			String text = (status < 400 ? conn.inputStream : conn.errorStream).getText('UTF-8')

			return [status: status, payload: new JsonSlurper().parseText(text)]
		}
		finally {
			conn.disconnect()
		}
	}

}
