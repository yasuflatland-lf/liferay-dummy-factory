package com.liferay.support.tools.it.util

import com.microsoft.playwright.APIRequestContext
import com.microsoft.playwright.APIResponse
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.RequestOptions

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Spock-friendly helper that POSTs to liferay-dummy-factory ResourceURLs via
 * a headless Chromium session.
 *
 * <p>
 * Building a portlet ResourceURL by hand is fragile on Liferay because
 * the URL depends on the runtime Layout and control-panel group resolution.
 * This helper instead uses Playwright to render the portlet once (exactly as
 * the browser does in the existing Functional specs), scrapes the real
 * {@code actionResourceURLs} the JSP hands to the React app, and POSTs to
 * those URLs using Playwright's APIRequestContext (which automatically
 * carries the session cookies captured from the login flow).
 * </p>
 *
 * <p>
 * The instance owns its own Playwright/Browser/Page and MUST be closed from
 * the calling spec's {@code cleanupSpec()}.
 * </p>
 */
class LdfResourceClient implements Closeable {

	static final String PORTLET_ID =
		'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'

	private static final String CONTROL_PANEL_PATH =
		'/group/control_panel/manage'

	private static final String NEW_PASSWORD = 'Test12345'

	private static final int REQUEST_TIMEOUT_MS = 60_000
	private static final int NAV_TIMEOUT_MS = 30_000

	private static final Logger log = LoggerFactory.getLogger(LdfResourceClient)

	private final String _baseUrl
	private final String _username
	private String _password

	private Playwright _playwright
	private Browser _browser
	private Page _page
	private String _authToken
	private boolean _loggedIn = false
	private final Map<String, String> _resourceUrlCache = [:]

	LdfResourceClient(
		String baseUrl, String username = 'test@liferay.com',
		String password = 'test') {

		_baseUrl = baseUrl?.endsWith('/') ?
			baseUrl.substring(0, baseUrl.length() - 1) : baseUrl
		_username = username
		_password = password
	}

	Map post(String mvcCommandName, Map<String, Object> fields) {
		_ensureLoggedIn()

		String jsonData = JsonOutput.toJson(fields ?: [:])

		String resourceURL = _resolveResourceURL(mvcCommandName)

		String body =
			"data=${_encode(jsonData)}" +
			(_authToken ? "&p_auth=${_encode(_authToken)}" : '')

		APIResponse response = _page.request().post(
			resourceURL,
			RequestOptions.create()
				.setHeader('Accept', 'application/json')
				.setHeader('Content-Type', 'application/x-www-form-urlencoded')
				.setTimeout(REQUEST_TIMEOUT_MS)
				.setData(body))

		int status = response.status()
		String responseBody = response.text() ?: ''

		if ((status < 200) || (status >= 300)) {
			throw new RuntimeException(
				"POST ${mvcCommandName} returned HTTP ${status}: " +
					_truncate(responseBody, 600))
		}

		if (!responseBody.trim()) {
			return [:]
		}

		def parsed = new JsonSlurper().parseText(responseBody)

		if (parsed instanceof Map) {
			return parsed as Map
		}

		return [result: parsed] as Map
	}

	Map createUser(Map<String, Object> fields) {
		return post('/ldf/user', fields)
	}

	Map createSite(Map<String, Object> fields) {
		return post('/ldf/site', fields)
	}

	Map createWebContent(Map<String, Object> fields) {
		return post('/ldf/wcm', fields)
	}

	Map createBlog(Map<String, Object> fields) {
		return post('/ldf/blog', fields)
	}

	/**
	 * Forces the internal login / password-change flow without invoking any
	 * MVC resource command. Callers that need to prime the admin password to
	 * the post-reset value before other helpers (JsonwsSetupHelper) fire can
	 * call this from {@code setupSpec()}.
	 */
	String login() {
		_ensureLoggedIn()

		return _password
	}

	@Override
	synchronized void close() {
		try {
			_page?.close()
		}
		catch (Exception ignored) {
		}

		try {
			_browser?.close()
		}
		catch (Exception ignored) {
		}

		try {
			_playwright?.close()
		}
		catch (Exception ignored) {
		}

		_page = null
		_browser = null
		_playwright = null
		_loggedIn = false
	}

	private synchronized void _ensureLoggedIn() {
		if (_loggedIn) {
			return
		}

		if (_playwright == null) {
			_playwright = Playwright.create()
			_browser = _playwright.chromium().launch(
				new BrowserType.LaunchOptions().setHeadless(true))
		}

		if (_page == null) {
			_page = _browser.newContext(
				new Browser.NewContextOptions().setViewportSize(1280, 720)
			).newPage()
		}

		_page.navigate("${_baseUrl}/")
		_page.waitForLoadState()

		_waitForLiferayGlobal(_page)

		String initialAuthToken = _page.evaluate('() => Liferay.authToken') as String

		// Try the initial password first, fall back to the post-reset password
		// so the helper survives both fresh containers (password == 'test')
		// and ones where we already ran through the password-change flow
		// (password == NEW_PASSWORD).
		List<String> candidatePasswords = [_password, NEW_PASSWORD]
			.findAll { it != null }
			.unique()

		boolean loggedInOk = false

		for (String pwd : candidatePasswords) {
			APIResponse loginResponse = _page.request().post(
				"${_baseUrl}/c/portal/login",
				RequestOptions.create()
					.setHeader('Content-Type', 'application/x-www-form-urlencoded')
					.setHeader('x-csrf-token', initialAuthToken ?: '')
					.setData(
						"login=${_encode(_username)}" +
						"&password=${_encode(pwd)}" +
						'&rememberMe=true'))

			if (loginResponse.status() == 200) {
				_password = pwd
				loggedInOk = true
				break
			}
		}

		if (!loggedInOk) {
			throw new RuntimeException(
				"Login failed for ${_username} with all candidate passwords")
		}

		_page.navigate("${_baseUrl}/")
		_page.waitForLoadState()

		// Liferay may still redirect to /c/portal/update_password after login
		// if another actor hasn't already consumed the flag.
		if (!_page.url().contains('/c/portal/update_password') &&
				!(_page.title()?.contains('New Password'))) {

			_waitForLiferayGlobal(_page)
		}

		// Liferay forces a password change on first login for the default admin
		// even when passwords.default.policy.change.required=false: the reset
		// happens because the admin's password has never been changed. Fill the
		// form with a stable value so subsequent navigations reach the portlet.
		if (_page.url().contains('/c/portal/update_password') ||
				_page.title()?.contains('New Password')) {

			_page.locator('#password1').fill(NEW_PASSWORD)
			_page.locator('#password2').fill(NEW_PASSWORD)
			_page.waitForNavigation({ ->
				_page.locator('[type=submit], button.btn-primary').first().click()
			})
			_password = NEW_PASSWORD

			_waitForLiferayGlobal(_page)
		}

		// Ignore the reminder-query prompt that some builds still render even
		// with users.reminder.query.enabled=false.
		if (_page.locator('#reminderQueryAnswer').isVisible()) {
			_page.locator('#reminderQueryAnswer').fill('test')
			_page.waitForNavigation({ ->
				_page.locator('[type=submit], button.btn-primary').first().click()
			})
		}

		_authToken = _page.evaluate('() => Liferay.authToken') as String
		_loggedIn = true

		log.info(
			'LdfResourceClient logged in as {} (p_auth={})',
			_username, _authToken ? 'present' : 'missing')
	}

	private String _resolveResourceURL(String mvcCommandName) {
		String cached = _resourceUrlCache[mvcCommandName]

		if (cached != null) {
			return cached
		}

		String renderURL =
			"${_baseUrl}${CONTROL_PANEL_PATH}" +
			"?p_p_id=${_encode(PORTLET_ID)}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'

		_page.navigate(
			renderURL,
			new Page.NavigateOptions().setTimeout(NAV_TIMEOUT_MS))
		_page.waitForLoadState()

		String html = _page.content()

		_parseActionResourceURLs(html).each { String name, String url ->
			_resourceUrlCache[name] = url.startsWith('http') ?
				url : "${_baseUrl}${url}"
		}

		String resolved = _resourceUrlCache[mvcCommandName]

		if (resolved == null) {
			throw new RuntimeException(
				"Could not find resource URL for ${mvcCommandName} in " +
					"portlet render. HTML (truncated): " +
					_truncate(html, 600))
		}

		return resolved
	}

	private static Map<String, String> _parseActionResourceURLs(String html) {
		Map<String, String> out = [:]

		// Extract every portlet resource URL the portlet container generated
		// for this render: URLs may be absolute or relative, but always
		// contain both p_p_lifecycle=2 and p_p_resource_id=<command>.
		//
		// Slashes may be escaped as \/ in HTML attribute or JSON contexts;
		// restore them after matching.
		String pattern =
			'((?:https?:\\\\?/\\\\?/[^"\'<>\\s]+?|/[^"\'<>\\s]*?)' +
				'p_p_lifecycle=2[^"\'<>\\s]*?' +
				'p_p_resource_id=([^"\'<>&\\s]+)[^"\'<>\\s]*)'

		def matcher = html =~ pattern

		while (matcher.find()) {
			String url = matcher.group(1).replace('\\/', '/')
			String rawCommand = matcher.group(2)
			String command = URLDecoder.decode(rawCommand, 'UTF-8')

			if (!out.containsKey(command)) {
				out[command] = url
			}
		}

		return out
	}

	private static String _truncate(String value, int max) {
		if (value == null) {
			return ''
		}

		if (value.length() <= max) {
			return value
		}

		return value.substring(0, max) + '...'
	}

	private static String _encode(String value) {
		return URLEncoder.encode(value ?: '', 'UTF-8')
	}

	/**
	 * DXP 2026 registers {@code window.Liferay} via a deferred module bootstrap,
	 * so the window-load event can fire before {@code Liferay.authToken} is
	 * populated. Poll the global before reading it so the evaluate call does
	 * not see {@code ReferenceError: Liferay is not defined}.
	 */
	private static void _waitForLiferayGlobal(Page page) {
		page.waitForFunction(
			'() => typeof window.Liferay !== "undefined" && ' +
				'!!window.Liferay.authToken')
	}

}
