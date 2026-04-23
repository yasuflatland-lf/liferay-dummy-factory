package com.liferay.support.tools.it.util

import groovy.json.JsonSlurper

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Spock-friendly helper that creates (and later deletes) precondition data
 * via Liferay JSONWS so that tests can assume a specific pre-state.
 *
 * Every create* method tracks the created record so {@link #cleanupAll()}
 * can delete everything in reverse insertion order. Exceptions raised during
 * cleanup are logged but never rethrown, so a failing teardown never masks
 * the real test failure.
 */
class JsonwsSetupHelper {

	/** Matches {@code com.liferay.portal.kernel.model.role.RoleConstants#TYPE_REGULAR}. */
	public static final int ROLE_TYPE_REGULAR = 1

	private static final String NEW_PASSWORD = 'Test12345'

	private static final Logger _log = LoggerFactory.getLogger(JsonwsSetupHelper)

	private final String _baseUrl
	private final String _username
	private final List<String> _candidatePasswords
	private final List<Tracked> _tracked = []

	JsonwsSetupHelper(
		String baseUrl, String username = 'test@liferay.com',
		String password = 'test') {

		_baseUrl = baseUrl.endsWith('/') ? baseUrl[0..-2] : baseUrl
		_username = username
		_candidatePasswords = [password, NEW_PASSWORD].findAll { it != null }.unique()
	}

	private String _basicAuthFor(String password) {
		return 'Basic ' + "${_username}:${password}".bytes.encodeBase64().toString()
	}

	Map createRole(String name, int type = ROLE_TYPE_REGULAR) {
		Map response = _post(
			'/api/jsonws/role/add-role',
			[
				'externalReferenceCode': '',
				'className': 'com.liferay.portal.kernel.model.Role',
				'classPK': '0',
				'name': name,
				'titleMap': _localizedJson(name),
				'descriptionMap': '{}',
				'type': type,
				'subtype': ''
			]) as Map

		_tracked << new Tracked(
			'role', '/api/jsonws/role/delete-role', 'roleId',
			response.roleId as Long)

		return response
	}

	Map createOrganization(String name) {
		// OrganizationService.addOrganization on DXP 2026 is exposed via JSONWS, but
		// form-encoded POSTs still tend to drop the ServiceContext structure. Route
		// through the headless-admin-user REST API for cleaner JSON body handling.
		// See docs/details/api-liferay-dxp2026.md §5.
		String body = "{\"name\":${_jsonQuote(name)}}"
		Map response = _postJson(
			'/o/headless-admin-user/v1.0/organizations', body) as Map

		Long organizationId = response.id as Long

		// The headless response nests the created entity; normalize the shape
		// so callers see a consistent JSONWS-style "organizationId" key.
		response.organizationId = organizationId

		_tracked << new Tracked(
			'organization', '/api/jsonws/organization/delete-organization',
			'organizationId', organizationId)

		return response
	}

	private static String _jsonQuote(String value) {
		String escaped = (value ?: '')
			.replace('\\', '\\\\')
			.replace('"', '\\"')

		return "\"${escaped}\""
	}

	private Object _postJson(String path, String jsonBody) {
		return _request(
			'Headless POST', 'POST', path, 'application/json', jsonBody)
	}

	Map createSite(String name, String membershipType = 'open') {
		int type = _siteTypeToConstant(membershipType)

		Map response = _post(
			'/api/jsonws/group/add-group',
			[
				'externalReferenceCode': '',
				'parentGroupId': '0',
				'liveGroupId': '0',
				'nameMap': _localizedJson(name),
				'descriptionMap': _localizedJson(''),
				'type': type,
				'typeSettings': '',
				'manualMembership': 'true',
				'membershipRestriction': '0',
				'friendlyURL': '/' + name.toLowerCase().replaceAll(/[^a-z0-9]+/, '-'),
				'site': 'true',
				'inheritContent': 'false',
				'active': 'true',
				'serviceContext': '{}'
			]) as Map

		_tracked << new Tracked(
			'group', '/api/jsonws/group/delete-group', 'groupId',
			response.groupId as Long)

		return response
	}

	Map createLayoutSetPrototype(String name) {
		Map response = _post(
			'/api/jsonws/layoutsetprototype/add-layout-set-prototype',
			[
				'nameMap': _localizedJson(name),
				'descriptionMap': _localizedJson(''),
				'active': 'true',
				'layoutsUpdateable': 'true',
				'serviceContext': '{}'
			]) as Map

		_tracked << new Tracked(
			'layoutSetPrototype',
			'/api/jsonws/layoutsetprototype/delete-layout-set-prototype',
			'layoutSetPrototypeId',
			response.layoutSetPrototypeId as Long)

		return response
	}

	/**
	 * Deletes every record created by this helper instance in reverse order of
	 * insertion. Cleanup errors are logged and swallowed so a failing teardown
	 * never hides the original test failure.
	 */
	void cleanupAll() {
		for (int i = _tracked.size() - 1; i >= 0; i--) {
			Tracked entry = _tracked[i]

			try {
				_post(entry.deletePath, entry.deleteParams())
			}
			catch (Exception e) {
				_log.warn(
					'JsonwsSetupHelper cleanup failed for {} (pk={}): {}',
					entry.kind, entry.primaryKey, e.message)
			}
		}

		_tracked.clear()
	}

	private static String _localizedJson(String value) {
		String escaped = value
			.replace('\\', '\\\\')
			.replace('"', '\\"')

		return /{"en_US":"${escaped}"}/
	}

	private static int _siteTypeToConstant(String membershipType) {
		switch (membershipType?.toLowerCase()) {
			case 'open': return 1
			case 'restricted': return 2
			case 'private': return 3
			default:
				throw new IllegalArgumentException(
					"Unknown site membership type: ${membershipType}")
		}
	}

	private Object _post(String path, Map<String, Object> params) {
		String body = params.collect { k, v ->
			"${URLEncoder.encode(k as String, 'UTF-8')}=" +
				"${URLEncoder.encode(v == null ? '' : v.toString(), 'UTF-8')}"
		}.join('&')

		return _request(
			'JSONWS POST', 'POST', path,
			'application/x-www-form-urlencoded', body)
	}

	/**
	 * Send an HTTP request with candidate-password retry. 401/403 triggers a
	 * retry with the next candidate password because the admin password may
	 * have been rotated (e.g. by the Playwright login flow in
	 * {@code LdfResourceClient}).
	 */
	private Object _request(
			String label, String method, String path, String contentType,
			String requestBody) {

		int lastStatus = 0
		String lastResponseBody = ''

		for (String password : _candidatePasswords) {
			String authHeader = _basicAuthFor(password)

			def conn = new URL(_baseUrl + path).openConnection() as HttpURLConnection

			try {
				conn.requestMethod = method
				conn.connectTimeout = 10_000
				conn.readTimeout = 30_000
				conn.setRequestProperty('Authorization', authHeader)
				conn.setRequestProperty('Accept', 'application/json')

				if (contentType) {
					conn.setRequestProperty('Content-Type', contentType)
				}

				if (requestBody != null) {
					conn.doOutput = true
					conn.outputStream.withWriter('UTF-8') { writer ->
						writer.write(requestBody)
					}
				}

				int status = conn.responseCode
				String responseBody = (status < 400)
					? (conn.inputStream?.text ?: '')
					: (conn.errorStream?.text ?: '')

				lastStatus = status
				lastResponseBody = responseBody

				if ((status == 401) || (status == 403)) {
					continue
				}

				if (status >= 400) {
					throw new IllegalStateException(
						"${label} ${path} returned HTTP ${status}: ${responseBody}")
				}

				if (!responseBody?.trim() || responseBody.trim() == 'null') {
					return null
				}

				return new JsonSlurper().parseText(responseBody)
			}
			finally {
				conn.disconnect()
			}
		}

		throw new IllegalStateException(
			"${label} ${path} returned HTTP ${lastStatus} for all " +
				"candidate passwords: ${lastResponseBody}")
	}

	private static class Tracked {

		final String kind
		final String deletePath
		final String primaryKeyParam
		final Object primaryKey

		Tracked(
			String kind, String deletePath, String primaryKeyParam,
			Object primaryKey) {

			this.kind = kind
			this.deletePath = deletePath
			this.primaryKeyParam = primaryKeyParam
			this.primaryKey = primaryKey
		}

		Map<String, Object> deleteParams() {
			return [(primaryKeyParam): primaryKey]
		}

	}

}
