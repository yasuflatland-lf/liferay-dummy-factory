package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.util.ProgressTracker;
import com.liferay.portal.kernel.util.ProgressTrackerThreadLocal;

import javax.portlet.ActionRequest;

/**
 * Progress bar mangaer
 * 
 * @author Yasuyuki Takeo
 *
 */
public class ProgressManager {

	private double _loader = 0;
	private double _loaderUnit = 10;
	private ProgressTracker _progressTracker;
	private long _threshold = 100;
	private ActionRequest _request;
	private long _sleep = 1500;
	
	/**
	 * Start Progress
	 * 
	 * @param request ActionRequest
	 * @param loaderInit 
	 */
	public void start(ActionRequest request, double loaderUnit) {
		_request = request;
		
		//Tracking progress start
		_progressTracker = new ProgressTracker("");
		ProgressTrackerThreadLocal.setProgressTracker(_progressTracker);
		_progressTracker.start(request);		
		
		if(0 >= loaderUnit) {
			_loader = _loaderUnit;
		} else {
			_loader = loaderUnit;
			_loaderUnit = loaderUnit;
		}
	}
	
	public long getThreshold() {
		return _threshold;
	}

	public void setThreshold(long threshold) {
		_threshold = threshold;
	}
	
	/**
	 * Tracking progress
	 * 
	 * @param index Index of loop
	 * @param numberOfTotal Total number
	 */
	public void trackProgress(long index, long numberOfTotal) {
		if (numberOfTotal >= _threshold) {
			if (index == (int) (numberOfTotal * (_loader / _threshold))) {
				System.out.println("Creating..." + (int) _loader + "% done");
				if(null != _progressTracker ) {
					_progressTracker.setPercent((int)_loader);
				}
				_loader = _loader + _loaderUnit;
			}
		}		
	}
	
	/**
	 * Finish progress bar
	 */
	public void finish() {
		try {
			//Progress bar doesn't hit if finish right away.
			Thread.sleep(_sleep);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		_progressTracker.finish(_request);			
	}
	
}
