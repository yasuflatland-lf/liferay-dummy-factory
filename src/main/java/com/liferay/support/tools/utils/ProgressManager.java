package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.ProgressTracker;
import com.liferay.portal.kernel.util.ProgressTrackerThreadLocal;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.ActionRequest;

/**
 * Progress bar mangaer
 * 
 * @author Yasuyuki Takeo
 *
 */
public class ProgressManager {

	private double _loader = 1;
	private ProgressTracker _progressTracker;
	private int _threshold = 100;
	private ActionRequest _request;
	private long _sleep = 1500;

	/**
	 * Start Progress
	 * 
	 * @param request    ActionRequest
	 * @param loaderInit
	 */
	public void start(ActionRequest request) {
		_request = request;

		String commonProgressId = ParamUtil.getString(request, LDFPortletKeys.COMMON_PROGRESS_ID,
				LDFPortletKeys.COMMON_PROGRESS_ID);

		// Tracking progress start
		_progressTracker = new ProgressTracker(commonProgressId);
		ProgressTrackerThreadLocal.setProgressTracker(_progressTracker);
		_progressTracker.start(request);

	}

	public int getThreshold() {
		return _threshold;
	}

	public void setThreshold(int threshold) {
		_threshold = threshold;
	}

	/**
	 * Calcluate the percentage of prgress
	 * 
	 * @param index
	 * @param numberOfTotal
	 * @return percentage of progress by int
	 */
	public int percentageCalcluation(long index, long numberOfTotal) {
		
		if(numberOfTotal <= 0) {
			return 0;
		}
		
		double dIndex  = (double)index;
		double dNumberOfTotal = (double)numberOfTotal;
		
		int result = (int)(dIndex / dNumberOfTotal * 100.00);
		
		return (_threshold <= result) ? _threshold : result;
	}
	
	/**
	 * Tracking progress
	 * 
	 * @param index         Index of loop
	 * @param numberOfTotal Total number
	 */
	public void trackProgress(long index, long numberOfTotal) {
		if(null != _progressTracker ) {
			_loader = percentageCalcluation(index, numberOfTotal);
			System.out.println("Creating..." + (int) _loader + "% done");
			_progressTracker.setPercent((int)_loader);
		}
	}

	/**
	 * Finish progress bar
	 */
	public void finish() {
		try {
			// Progress bar doesn't hit if finish right away.
			Thread.sleep(_sleep);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		_progressTracker.finish(_request);
	}

}
