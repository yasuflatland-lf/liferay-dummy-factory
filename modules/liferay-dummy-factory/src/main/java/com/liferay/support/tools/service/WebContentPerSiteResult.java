package com.liferay.support.tools.service;

public record WebContentPerSiteResult(
	long groupId, String siteName, int created, int failed, String error) {

	public WebContentPerSiteResult {
		if (groupId <= 0) {
			throw new IllegalArgumentException("groupId must be > 0");
		}

		if ((created < 0) || (failed < 0)) {
			throw new IllegalArgumentException(
				"created and failed must be non-negative");
		}

		if (siteName == null) {
			throw new IllegalArgumentException("siteName is required");
		}

		if ((error != null) && (failed == 0)) {
			throw new IllegalArgumentException(
				"error requires failed > 0");
		}

		if ((failed > 0) && ((error == null) || error.isBlank())) {
			throw new IllegalArgumentException(
				"error must be set when failed > 0");
		}
	}

}
