package com.liferay.support.tools.portlet.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
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
			new PrincipalException("denied"), "/ldf/user",
			"Creating data requires a company administrator.");
	}

	@Test
	void omniadminDenialIsLocalizedAndLoggedWithoutStackTrace() {
		PermissionChecker permissionChecker = _proxy(
			PermissionChecker.class,
			(proxy, method, args) -> {
				if (method.getReturnType() == long.class) {
					return 42L;
				}

				return false;
			});

		_assertDenial(
			new PrincipalException.MustBeOmniadmin(permissionChecker),
			"/ldf/company", "Creating companies requires an omniadmin.");
	}

	private void _assertDenial(
		PrincipalException principalException, String command,
		String expectedMessage) {

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

		List<String> warnings = new ArrayList<>();

		Log log = _proxy(
			Log.class,
			(proxy, method, args) -> {
				assertEquals("warn", method.getName());
				assertEquals(1, args.length);

				warnings.add((String)args[0]);

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

			PortletJsonCommandTemplate.permissionDenied(
				resourceRequest, responseJson, log, principalException);
		}
		finally {
			languageUtil.setLanguage(originalLanguage);
		}

		assertEquals(
			Map.of("success", false, "error", expectedMessage), response);
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("userId=42"));
		assertTrue(warnings.get(0).contains("command=" + command));
	}

	private static <T> T _proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(
			Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type}, handler));
	}

}
