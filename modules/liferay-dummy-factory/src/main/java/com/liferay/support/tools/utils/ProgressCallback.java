package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

@FunctionalInterface
public interface ProgressCallback {

	void onProgress(long current, long total);

	static final ProgressCallback NOOP = (c, t) -> {};

	static ProgressCallback fromProgressManager(ProgressManager manager) {
		if (manager == null) {
			return NOOP;
		}

		return (current, total) -> {
			try {
				manager.trackProgress(current, total);
			}
			catch (Exception e) {
				_log.warn("Progress tracking failed", e);
			}
		};
	}

	static final Log _log = LogFactoryUtil.getLog(ProgressCallback.class);

}
