package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.GogoShellClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.RequestOptions

import groovy.json.JsonSlurper

import org.jacoco.core.tools.ExecDumpClient

import spock.lang.Shared
import spock.lang.Specification

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class BaseLiferaySpec extends Specification {

	protected static final String PORTLET_ID = 'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'

	protected static final String JSONWS_BASE = '/api/jsonws/'

	/**
	 * Password submitted to the first-login {@code update_password} form.
	 * DXP 2026 forces the default admin through the form regardless of
	 * {@code passwords.default.policy.change.required}, so any Playwright
	 * login must either submit this form or be turned back. We always submit
	 * it with this value so subsequent specs hit a stable admin password.
	 */
	protected static final String NEW_ADMIN_PASSWORD = 'Test12345'

	private static final Logger log = LoggerFactory.getLogger(BaseLiferaySpec)

	@Shared
	static LiferayContainer liferay = LiferayContainer.getInstance()

	@Shared
	static boolean bundleVerified = false

	@Shared
	static Long cachedCompanyId = null

	static Path getModuleJarPath() {
		Path jarDir = Path.of(System.getProperty('user.dir')).parent.resolve(
			'modules/liferay-dummy-factory/build/libs')

		File jar = jarDir.toFile().listFiles()?.find { File f ->
			f.name.endsWith('.jar') && f.name.contains('liferay.dummy.factory')
		}

		if (jar == null) {
			throw new IllegalStateException(
				"Module JAR not found in ${jarDir}. " +
				"Run './gradlew :modules:liferay-dummy-factory:build' first."
			)
		}

		return jar.toPath()
	}

	static synchronized void ensureBundleActive() {
		if (bundleVerified) {
			return
		}

		log.info('Deploying JAR: {}', getModuleJarPath())
		liferay.deployJar(getModuleJarPath())
		log.info('JAR copied to container. GoGo Shell at {}:{}', liferay.host, liferay.gogoPort)

		boolean active = false

		for (int i = 0; i < 60; i++) {
			try {
				new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
					String output = gogo.execute('lb')
					def allLines = output.readLines()
					int lineCount = allLines.size()
					def tail = allLines.takeRight(5)
					log.info('GoGo Shell attempt {}: {} lines, last 5: {}', i + 1, lineCount, tail)
					def lines = allLines.findAll {
						it.toLowerCase().contains('liferay') &&
							it.toLowerCase().contains('dummy') &&
							it.toLowerCase().contains('factory')
					}
					log.info('Matches: {}', lines ?: '(no match)')

					if (lines.any { it.contains('Active') }) {
						active = true
					}
				}

				if (active) {
					break
				}
			}
			catch (Exception e) {
				log.warn('GoGo Shell attempt {} failed: {}', i + 1, e.message)
			}

			TimeUnit.SECONDS.sleep(5)
		}

		if (!active) {
			throw new IllegalStateException(
				'Bundle liferay.dummy.factory did not reach ACTIVE ' +
				'state within timeout'
			)
		}

		bundleVerified = true
	}

	/**
	 * Establish a Playwright-side authenticated session as the default admin.
	 *
	 * With D2 (portal-ext.properties suppressing PASSWORDRESET, terms-of-use,
	 * and reminder queries) the post-login flow is a single form POST with no
	 * password-reset detour. Kept as a protected method because many specs
	 * call it from setupSpec() to prime a Playwright session used later for
	 * UI-driven assertions.
	 */
	protected static void loginAsAdmin(PlaywrightLifecycle pw) {
		Page page = pw.newPage()

		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		// DXP 2026 defers window.Liferay through a module bootstrap, so the
		// load event can fire before Liferay.authToken is populated.
		page.waitForFunction(
			'() => typeof window.Liferay !== "undefined" && ' +
				'!!window.Liferay.authToken')

		String authToken = page.evaluate('() => Liferay.authToken') as String

		// After a prior spec has submitted /c/portal/update_password the admin
		// password is no longer DEFAULT_ADMIN_PASSWORD. Try candidates in
		// order; the first one that returns a success status wins.
		List<String> candidatePasswords = [
			LiferayContainer.DEFAULT_ADMIN_PASSWORD, NEW_ADMIN_PASSWORD
		].findAll { it != null }.unique()

		int lastStatus = -1
		String lastBody = ''

		for (String pwd : candidatePasswords) {
			def response = page.request().post(
				"${liferay.baseUrl}/c/portal/login",
				RequestOptions.create()
					.setHeader(
						'Content-Type', 'application/x-www-form-urlencoded')
					.setHeader('x-csrf-token', authToken)
					.setData(
						"login=${URLEncoder.encode(LiferayContainer.DEFAULT_ADMIN_EMAIL, 'UTF-8')}" +
						"&password=${URLEncoder.encode(pwd, 'UTF-8')}" +
						'&rememberMe=true'
					)
			)

			lastStatus = response.status()

			if (lastStatus == 200 || lastStatus == 302) {
				break
			}

			lastBody = response.text()?.take(500) ?: ''
		}

		if (lastStatus != 200 && lastStatus != 302) {
			throw new IllegalStateException(
				"loginAsAdmin: portal login returned HTTP ${lastStatus} " +
				"(expected 200 or 302). Body preview: ${lastBody}"
			)
		}

		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		// DXP 2026 forces the default admin through /c/portal/update_password
		// on first login regardless of passwords.default.policy.change.required.
		// Fill the form so the browser reaches a normal page and the session
		// is not stuck on the reset screen for later portlet navigations.
		if (page.url().contains('/c/portal/update_password')) {
			page.locator('#password1').fill(NEW_ADMIN_PASSWORD)
			page.locator('#password2').fill(NEW_ADMIN_PASSWORD)

			page.locator('[type=submit], button.btn-primary').first().click()

			page.waitForLoadState(LoadState.NETWORKIDLE)
		}
	}

	protected String jsonwsUrl(String path) {
		return "${LiferayContainer.getInstance().baseUrl}${JSONWS_BASE}" +
			path.replaceFirst('^/', '')
	}

	protected static int httpGet(String url) {
		def connection = new URL(url).openConnection() as HttpURLConnection

		connection.requestMethod = 'GET'
		connection.connectTimeout = 30_000
		connection.readTimeout = 30_000

		return connection.responseCode
	}

	protected Map headlessGet(String path) {
		return _httpGet(path, 'application/json') { status, body ->
			if (status >= 400) {
				throw new IllegalStateException(
					"headlessGet ${path} returned HTTP ${status}: ${body}")
			}

			return new JsonSlurper().parseText(body) as Map
		}
	}

	protected Object jsonwsGet(String path) {
		return _httpGet(jsonwsUrl(path), 'application/json') { status, body ->
			if (status >= 400) {
				throw new IllegalStateException(
					"jsonwsGet ${path} returned HTTP ${status}: ${body}")
			}

			if (!body?.trim() || body.trim() == 'null') {
				return null
			}

			return new JsonSlurper().parseText(body)
		}
	}

	protected Object jsonwsPost(String path, Map<String, Object> params) {
		String body = params.collect { k, v ->
			"${URLEncoder.encode(k as String, 'UTF-8')}=" +
				"${URLEncoder.encode(v == null ? '' : v.toString(), 'UTF-8')}"
		}.join('&')

		return _httpPost(
				jsonwsUrl(path), 'application/json',
				'application/x-www-form-urlencoded', body) { status, responseBody ->

			if (status >= 400) {
				throw new IllegalStateException(
					"jsonwsPost ${path} returned HTTP ${status}: ${responseBody}")
			}

			if (!responseBody?.trim() || responseBody.trim() == 'null') {
				return null
			}

			return new JsonSlurper().parseText(responseBody)
		}
	}

	protected Long getCompanyId() {
		if (cachedCompanyId == null) {
			def user = jsonwsGet('user/get-current-user') as Map
			cachedCompanyId = user.companyId as Long
		}

		return cachedCompanyId
	}

	protected Map headlessPost(String path, String jsonBody) {
		return _httpPost(
				absoluteUrl(path), 'application/json', 'application/json',
				jsonBody) { status, body ->

			if (status >= 400) {
				throw new IllegalStateException(
					"headlessPost ${path} returned HTTP ${status}: ${body}")
			}

			return new JsonSlurper().parseText(body) as Map
		}
	}

	protected int headlessDelete(String path) {
		def conn = new URL(absoluteUrl(path)).openConnection() as HttpURLConnection

		try {
			conn.requestMethod = 'DELETE'
			conn.connectTimeout = 10_000
			conn.readTimeout = 30_000
			conn.setRequestProperty('Authorization', basicAuthHeader())
			conn.setRequestProperty('Accept-Encoding', 'identity')

			return conn.responseCode
		}
		finally {
			conn.disconnect()
		}
	}

	protected String absoluteUrl(String path) {
		if (path.startsWith('http://') || path.startsWith('https://')) {
			return path
		}
		return "${liferay.baseUrl}${path.startsWith('/') ? '' : '/'}${path}"
	}

	private Object _httpGet(
			String pathOrUrl, String acceptType, Closure<Object> responseHandler) {

		return _request('GET', pathOrUrl, acceptType, null, null, responseHandler)
	}

	private Object _httpPost(
			String pathOrUrl, String acceptType, String contentType,
			String requestBody, Closure<Object> responseHandler) {

		return _request(
			'POST', pathOrUrl, acceptType, contentType, requestBody,
			responseHandler)
	}

	private Object _request(
			String method, String pathOrUrl, String acceptType,
			String contentType, String requestBody,
			Closure<Object> responseHandler) {

		String url = absoluteUrl(pathOrUrl)
		def conn = new URL(url).openConnection() as HttpURLConnection

		try {
			conn.requestMethod = method
			conn.connectTimeout = 10_000
			conn.readTimeout = 30_000
			conn.setRequestProperty('Authorization', basicAuthHeader())
			conn.setRequestProperty('Accept-Encoding', 'identity')

			if (acceptType) {
				conn.setRequestProperty('Accept', acceptType)
			}

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
			String body = (status < 400)
				? (conn.inputStream?.text ?: '')
				: (conn.errorStream?.text ?: '')

			return responseHandler.call(status, body)
		}
		finally {
			conn.disconnect()
		}
	}

	/**
	 * Returns a Basic-Auth header for the default admin. DXP 2026 forces a
	 * password change on first admin login (see {@link #loginAsAdmin}), and
	 * every spec that exercises JSONWS first goes through either
	 * {@code loginAsAdmin} or {@code LdfResourceClient.login}, both of which
	 * submit the {@code update_password} form with {@link #NEW_ADMIN_PASSWORD}.
	 * Using the post-reset value here avoids the silent-Guest fallback that
	 * happens when {@code BasicAuthHeaderAutoLoginSupport.doLogin} authenticates
	 * with the wrong password and {@code BasicAuthHeaderAuthVerifier} returns an
	 * empty {@code AuthVerifierResult} (no challenge issued because
	 * {@code forceBasicAuth} is unset on {@code BasicAuthHeaderAuthVerifierConfiguration}).
	 */
	protected String basicAuthHeader() {
		String credentials =
			"${LiferayContainer.DEFAULT_ADMIN_EMAIL}:${NEW_ADMIN_PASSWORD}"

		return "Basic ${credentials.bytes.encodeBase64().toString()}"
	}

	def cleanupSpec() {
		try {
			dumpJacocoCoverage(this.class.simpleName)
		}
		catch (Exception e) {
			log.warn('JaCoCo dump failed for {}: {}', this.class.simpleName, e.message, e)
		}
	}

	protected void dumpJacocoCoverage(String specName) {
		if (!liferay.isRunning()) {
			log.warn('Skipping JaCoCo dump for {} — container is not running', specName)
			return
		}

		File outputFile = new File(System.getProperty('user.dir'), "build/jacoco/${specName}.exec")
		File jacocoDir = outputFile.parentFile
		if (!jacocoDir.mkdirs() && !jacocoDir.isDirectory()) {
			throw new IOException("Cannot create JaCoCo output directory: ${jacocoDir.absolutePath}")
		}

		Exception lastEx = null
		for (int i = 0; i < 3; i++) {
			try {
				new ExecDumpClient().dump(liferay.host, liferay.jacocoPort).save(outputFile, false)
				log.info('JaCoCo coverage dumped to {} ({} bytes)', outputFile.absolutePath, outputFile.length())
				return
			}
			catch (Exception e) {
				lastEx = e
				if (i < 2) {
					TimeUnit.SECONDS.sleep(2)
				}
			}
		}
		throw lastEx
	}

}
