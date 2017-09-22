package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Reference;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.url.WebURL;

/*
 * This class shows how you can crawl images on the web and store them in a
 * folder. This is just for demonstration purposes and doesn't scale for large
 * number of images. For crawling millions of images you would need to store
 * downloaded images in a hierarchy of folders
 */
public class ImageCrawler extends WebCrawler {

	private static final Pattern imgPatterns = Pattern.compile(".*(\\.(gif|jpe?g|png|tiff?))$");

	private static String[] crawlDomains;
	
	public static void configure(String[] domain) {
		crawlDomains = domain;
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();

		if (imgPatterns.matcher(href).matches()) {
			return true;
		}

		for (String domain : crawlDomains) {
			if (href.startsWith(domain)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();

		// We are only interested in processing images which are bigger than 10k
		if (!imgPatterns.matcher(url).matches() || !((page.getParseData() instanceof BinaryParseData)
				|| (page.getContentData().length < (10 * 1024)))) {
			return;
		}

		_imageCrawlController.setURL(url);
		_log.error("URL : " + url);
	}

	@Reference
	private ImageCrawlController _imageCrawlController;
	
	private static final Log _log = LogFactoryUtil.getLog(ImageCrawler.class);
	

}