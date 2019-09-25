package com.liferay.support.tools.common;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Time recorder
 * 
 * @author Yasuyuki Takeo
 */
public class TimeKeeper {
	protected StopWatch _stopWatch = new StopWatch();

	public void start() {
		_stopWatch.start();
	}

	public void stop() {
		_stopWatch.stop();
	}

	public void outputTime() {
		long miliTime = _stopWatch.getTime(TimeUnit.MILLISECONDS);
		long minTime = _stopWatch.getTime(TimeUnit.MINUTES);

		StringBuffer sb = new StringBuffer();
		sb.append("-> Elapsed time ");
		sb.append(String.valueOf(miliTime) + " mili sec");
		sb.append(" (" + String.valueOf(minTime) + " min )");

		System.out.println(sb.toString());
	}
}
