package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public final class ScreenNameSanitizer {

	/**
	 * Sanitize raw text for use as a Liferay screen name.
	 *
	 * Rules:
	 * 1. null input → returns "user"
	 * 2. Remove every character not in [a-zA-Z0-9._-]
	 * 3. Collapse runs of '.' into a single '.'
	 * 4. Strip leading and trailing '.', '-', '_'
	 * 5. If the result is empty after step 4, return "user" (logged at WARN)
	 *
	 * The caller is responsible for lowercasing and for appending
	 * a disambiguating index suffix. This method does NOT lowercase.
	 */
	public static String sanitize(String input) {
		if (input == null) {
			_log.warn(
				"ScreenNameSanitizer received null input; substituting 'user' fallback");
			return "user";
		}

		String result = input.replaceAll("[^a-zA-Z0-9._-]", "");

		result = result.replaceAll("\\.{2,}", ".");

		result = result.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");

		if (result.isEmpty()) {
			_log.warn(
				"ScreenNameSanitizer stripped '" + input +
					"' to empty; substituting 'user' fallback");
			return "user";
		}

		return result;
	}

	private ScreenNameSanitizer() {}

	private static final Log _log = LogFactoryUtil.getLog(
		ScreenNameSanitizer.class);

}
