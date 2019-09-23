package com.liferay.support.tools.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	private static String crawlDomain;

	public static void configure(String domain) {
		crawlDomain = domain;
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();

		if (PATTERNS.matcher(href).matches()) {
			return true;
		}

		if (href.startsWith(crawlDomain)) {
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

		gatheredURLs.add(url);
		System.out.println("Fetched URL : " + url);
	}

	@Override
	public Object getMyLocalData() {
		return gatheredURLs;
	}

	private List<String> gatheredURLs = Collections.synchronizedList(new ArrayList<>());
	

}