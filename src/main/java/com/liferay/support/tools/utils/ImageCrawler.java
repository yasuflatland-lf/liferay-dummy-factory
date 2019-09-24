package com.liferay.support.tools.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Image Link Crawler
 * 
 * @author Yasuyuki Takeo
 *
 */
public class ImageCrawler extends WebCrawler {

	private static final Pattern PATTERNS = Pattern.compile(".*(\\.(gif|jpe?g|png|tiff?))$");

	public static void configure(String domain, long amount) {
		_crawlDomain = domain;
		_amount.set(amount);
		_imgCount.set(0);
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();

		if (PATTERNS.matcher(href).matches()) {
			return true;
		}

		if (href.startsWith(_crawlDomain)) {
			return true;
		}

		return false;
	}

	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();

		// We are only interested in processing images which are bigger than 10k
		if (!PATTERNS.matcher(url).matches() || 
			!((page.getParseData() instanceof BinaryParseData) ||
			(page.getContentData().length < (10 * 1024)))) {
			return;
		}

		if(_amount.get() <= _imgCount.get() ) {
			myController.shutdown();
			return;
		}

		gatheredURLs.add(url);
		_imgCount.incrementAndGet();
		
		System.out.println("Fetched URL : " + url);
	}

	@Override
	public Object getMyLocalData() {
		return gatheredURLs;
	}
	
	private static String _crawlDomain;
	private static AtomicLong _amount = new AtomicLong();
	private static AtomicLong _imgCount = new AtomicLong();

	private List<String> gatheredURLs = Collections.synchronizedList(new ArrayList<>());
	

}