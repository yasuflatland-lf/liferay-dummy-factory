package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.ProgressTracker;
import com.liferay.portal.kernel.util.ProgressTrackerThreadLocal;
import com.liferay.support.tools.constants.LDFPortletKeys;

import jakarta.portlet.PortletRequest;

public class ProgressManager {

	public void start(PortletRequest request) {
		_request = request;

		String commonProgressId = ParamUtil.getString(
			request, LDFPortletKeys.COMMON_PROGRESS_ID,
			LDFPortletKeys.COMMON_PROGRESS_ID);

		_progressTracker = new ProgressTracker(commonProgressId);
		ProgressTrackerThreadLocal.setProgressTracker(_progressTracker);
		_progressTracker.start(request);
	}

	public int percentageCalculation(long index, long numberOfTotal) {
		if (numberOfTotal <= 0) {
			return 0;
		}

		int result = (int)((double)index / numberOfTotal * 100.0);

		return Math.min(result, _THRESHOLD);
	}

	public void trackProgress(long index, long numberOfTotal) {
		if (_progressTracker != null) {
			_progressTracker.setPercent(
				percentageCalculation(index, numberOfTotal));
		}
	}

	public void finish() {
		if ((_progressTracker != null) && (_request != null)) {
			try {
				Thread.sleep(_sleep);
			}
			catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
			}

			_progressTracker.finish(_request);
		}
	}

	private ProgressTracker _progressTracker;
	private PortletRequest _request;
	private long _sleep = 1500;
	private static final int _THRESHOLD = 100;

}
