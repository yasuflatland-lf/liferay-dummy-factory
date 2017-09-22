package com.liferay.support.tools.utils;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

@Component(service = ImageCrawlController.class)
public class ImageCrawlController {

	public static void main(String[] args) throws Exception {

		ImageCrawlController.run(15, 20, new String[] { "https://imgur.com/","https://www.shutterstock.com/photos" });
		List<String> results = ImageCrawlController.getURL();
		results.stream()
		  .map(s -> "[" + s + "]")
		  .forEach(System.out::println);
	}

	public static void run(int numberOfCrawlers, int secForCrawling, String[] crawlDomains) throws Exception {

		CrawlConfig config = new CrawlConfig();

		File tempDir = Files.createTempDir();
		config.setCrawlStorageFolder(tempDir.getAbsolutePath());
		config.setMaxDepthOfCrawling(1);		
		config.setMaxPagesToFetch(100);

		/*
		 * Since images are binary content, we need to set this parameter to
		 * true to make sure they are included in the crawl.
		 */
		config.setIncludeBinaryContentInCrawling(true);

		if (0 == crawlDomains.length) {
			throw new InvalidParameterException("crawlDomains must be more than one URL");
		}

		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

		for (String domain : crawlDomains) {
			controller.addSeed(domain);
		}

		ImageCrawler.configure(crawlDomains);

		controller.startNonBlocking(ImageCrawler.class, numberOfCrawlers);

		// Wait for secForCrawling seconds
		Thread.sleep(secForCrawling * 1000);

		// Send the shutdown request and then wait for finishing
		controller.shutdown();
		controller.waitUntilFinish();

	}

	public static void setURL(String url) {
		gatheredURLs.add(url);
	}
	
	public static List<String> getURL() {
		return Lists.newArrayList(gatheredURLs);
	}
	
	private static final List<String> gatheredURLs = Collections.synchronizedList(new ArrayList<>());
}