package com.liferay.support.tools.utils;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * Image Crawl Controller
 * <p/>
 * This class manage crawlers for fetching dummy links from Internet
 *
 * @author Yasuyuki Takeo
 */
@Component(immediate = true, service = ImageCrawlController.class)
public class ImageCrawlController {

    public void exec(
        int numberOfCrawlers, int maxDepthOfCrawling, int maxPagesToFetch,
        String domain) throws Exception {

        CrawlConfig config = new CrawlConfig();


        // Set the folder where intermediate crawl data is stored (e.g. list of urls that are extracted from previously
        // fetched pages and need to be crawled later).
        File tempDir = Files.createTempDir();
        config.setCrawlStorageFolder(tempDir.getAbsolutePath());
        
        config.setPolitenessDelay(1000);
        config.setMaxDepthOfCrawling(maxDepthOfCrawling);
        config.setMaxPagesToFetch(maxPagesToFetch);

		/*
         * Since images are binary content, we need to set this parameter to
		 * true to make sure they are included in the crawl.
		 */
        config.setIncludeBinaryContentInCrawling(true);
        
        // Enable SSL
        config.setIncludeHttpsPages(true);

        PageFetcher     pageFetcher     = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller      = new CrawlController(config, pageFetcher, robotstxtServer);

        controller.addSeed(domain);

        ImageCrawler.configure(domain);

        //Start crawling
        controller.startNonBlocking(ImageCrawler.class, numberOfCrawlers);

        controller.waitUntilFinish();

        // Correcting URLs from each crawlers' results.
        List<Object> crawlersLocalData = controller.getCrawlersLocalData();
        for (Object localData : crawlersLocalData) {
            @SuppressWarnings("unchecked")
            List<String> urlLists = (List<String>) (localData);
            gatheredURLs.addAll(urlLists);
        }
        
    }

    public List<String> getURL() {
        return Lists.newArrayList(gatheredURLs);
    }

    private List<String> gatheredURLs = Collections.synchronizedList(new ArrayList<>());
}