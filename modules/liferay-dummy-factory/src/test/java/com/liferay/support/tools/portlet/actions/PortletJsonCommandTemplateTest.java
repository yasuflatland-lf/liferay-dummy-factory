package com.liferay.support.tools.portlet.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.WebKeys;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.portlet.PortletConfig;
import jakarta.portlet.ResourceRequest;

import org.junit.jupiter.api.Test;

class PortletJsonCommandTemplateTest {

	@Test
	void companyAdminDenialIsLocalizedAndLoggedWithoutStackTrace() {
		_assertDenial(
			new PrincipalException.MustBeCompanyAdmin(42L), "/ldf/user",
			"Creating data requires a company administrator.");
	}

	@Test
	void illegalArgumentIsReturnedWithoutLogging() {
		List<String> logCalls = new ArrayList<>();

		Map<String, Object> response = _handleFailure(
			new IllegalArgumentException("count must be positive"), logCalls);

		assertEquals(
			Map.of("success", false, "error", "count must be positive"),
			response);
		assertEquals(List.of(), logCalls);
	}

	@Test
	void omniadminDenialIsLocalizedAndLoggedWithoutStackTrace() {
		_assertDenial(
			new PrincipalException.MustBeOmniadmin(42L), "/ldf/company",
			"Creating companies requires an omniadmin.");
	}

	@Test
	void otherPrincipalExceptionStaysOnTheErrorPath() {
		_assertError(
			new PrincipalException.MustHavePermission(42L, "ADD_ENTRY"));
	}

	@Test
	void unexpectedFailureIsLoggedWithStackTrace() {
		_assertError(new RuntimeException("boom"));
	}

	private void _assertDenial(
		PrincipalException principalException, String command,
		String expectedMessage) {

		List<String> logCalls = new ArrayList<>();

		Map<String, Object> response = _handleFailure(
			principalException, command, logCalls);

		assertEquals(
			Map.of("success", false, "error", expectedMessage), response);
		assertEquals(1, logCalls.size());

		String warning = logCalls.get(0);

		assertTrue(warning.startsWith("warn:"), warning);
		assertTrue(warning.contains("userId=42"), warning);
		assertTrue(warning.contains("command=" + command), warning);
		assertTrue(
			warning.contains("reason=" + principalException.getMessage()),
			warning);
	}

	private void _assertError(Throwable throwable) {
		List<String> logCalls = new ArrayList<>();

		Map<String, Object> response = _handleFailure(throwable, logCalls);

		assertEquals(
			Map.of("success", false, "error", throwable.getMessage()), response);
		assertEquals(List.of("error:" + _ERROR_LOG_MESSAGE), logCalls);
	}

	private Map<String, Object> _handleFailure(
		Throwable throwable, List<String> logCalls) {

		return _handleFailure(throwable, "/ldf/user", logCalls);
	}

	private Map<String, Object> _handleFailure(
		Throwable throwable, String command, List<String> logCalls) {

		ThemeDisplay themeDisplay = new ThemeDisplay();

		themeDisplay.setUser(_proxy(User.class, (proxy, method, args) -> 42L));

		ResourceBundle resourceBundle = ResourceBundle.getBundle(
			"content.Language", Locale.US);

		PortletConfig portletConfig = _proxy(
			PortletConfig.class, (proxy, method, args) -> resourceBundle);

		ResourceRequest resourceRequest = _proxy(
			ResourceRequest.class,
			(proxy, method, args) -> {
				if (method.getName().equals("getAttribute")) {
					if (WebKeys.THEME_DISPLAY.equals(args[0])) {
						return themeDisplay;
					}

					assertEquals(JavaConstants.JAKARTA_PORTLET_CONFIG, args[0]);

					return portletConfig;
				}

				if (method.getName().equals("getLocale")) {
					return Locale.US;
				}

				return command;
			});

		Map<String, Object> response = new HashMap<>();

		JSONObject responseJson = _proxy(
			JSONObject.class,
			(proxy, method, args) -> {
				response.put((String)args[0], args[1]);

				return proxy;
			});

		Log log = _proxy(
			Log.class,
			(proxy, method, args) -> {
				if (method.getName().equals("warn")) {
					assertEquals(1, args.length);
				}
				else {
					assertEquals("error", method.getName());
					assertEquals(2, args.length);
					assertSame(throwable, args[1]);
				}

				logCalls.add(method.getName() + ":" + args[0]);

				return null;
			});

		LanguageUtil languageUtil = new LanguageUtil();

		Language originalLanguage = LanguageUtil.getLanguage();

		try {
			languageUtil.setLanguage(
				_proxy(
					Language.class,
					(proxy, method, args) -> ((ResourceBundle)args[0]).getString(
						(String)args[1])));

			PortletJsonCommandTemplate.handleFailure(
				resourceRequest, responseJson, log, _ERROR_LOG_MESSAGE, throwable);
		}
		finally {
			languageUtil.setLanguage(originalLanguage);
		}

		return response;
	}

	private static <T> T _proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(
			Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type}, handler));
	}

	private static final String _ERROR_LOG_MESSAGE = "Failed to create users";

}
