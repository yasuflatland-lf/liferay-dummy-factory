package com.liferay.support.tools.portlet.actions;

import com.goikosoft.crawler4j.crawler.CrawlConfig;
import com.goikosoft.crawler4j.crawler.CrawlController;
import com.goikosoft.crawler4j.fetcher.PageFetcher;
import com.goikosoft.crawler4j.robotstxt.RobotstxtConfig;
import com.goikosoft.crawler4j.robotstxt.RobotstxtServer;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ImageCrawler;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Image Links generators
 *
 * @author Yasuyuki Takeo
 */
@Component(
    immediate = true,
    property = {
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
        "javax.portlet.name=" + LDFPortletKeys.PORTLET_CONFIGURATION,
        "mvc.command.name=/ldf/image/list"
    },
    service = MVCResourceCommand.class
)
public class ImageLinksMVCResourceCommand extends BaseMVCResourceCommand {

  private static final Log _log = LogFactoryUtil.getLog(ImageLinksMVCResourceCommand.class);

  @Reference
  private Portal _portal;

  @Override
  protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
      throws Exception {
    int numberOfCrawlers = ParamUtil.getInteger(resourceRequest, "numberOfCrawlers", -1);
    int maxDepthOfCrawling = ParamUtil.getInteger(resourceRequest, "maxDepthOfCrawling", -1);
    int maxPagesToFetch = ParamUtil.getInteger(resourceRequest, "maxPagesToFetch", -1);
    int randomAmount = ParamUtil.getInteger(resourceRequest, "randomAmount", 10);

    String tmpUrls = ParamUtil
        .getString(resourceRequest, "urls", "https://imgur.com/search?q=flower");
    String[] strArray = tmpUrls.split(",");
    List<String> urls = new ArrayList<>(Arrays.asList(strArray));
    List<String> result = Lists.newArrayList();

    if (_log.isDebugEnabled()) {
      _log.debug("numberOfCrawlers : " + String.valueOf(numberOfCrawlers));
      _log.debug("maxDepthOfCrawling : " + String.valueOf(maxDepthOfCrawling));
      _log.debug("maxPagesToFetch : " + String.valueOf(maxPagesToFetch));
    }

    if (numberOfCrawlers >= 0 &&
        maxDepthOfCrawling >= 0 &&
        maxPagesToFetch >= 0) {

      // Run image links crawler
      result = runEx(
          numberOfCrawlers,
          maxDepthOfCrawling,
          maxPagesToFetch,
          urls,
          randomAmount
      ).blockingSingle();
    }

    JSONObject jsonObject = createReturnJson(resourceRequest, resourceResponse, result);

    JSONPortletResponseUtil.writeJSON(
        resourceRequest, resourceResponse, jsonObject);

  }

  /**
   * Run Image links crawler
   *
   * @param numberOfCrawlers   Number of crawlers to run
   * @param maxDepthOfCrawling Page link depth for crawling
   * @param maxPagesToFetch    Max pages to fetch
   * @param urls               Target site top page urls
   * @param randomAmount       amount of data to fetch
   * @throws Exception
   */
  public Flowable<ArrayList<String>> runEx(
      int numberOfCrawlers, int maxDepthOfCrawling,
      int maxPagesToFetch,
      List<String> urls, int randomAmount) {

    return Flowable.fromIterable(urls)
        .concatMap(url -> {
          // Get images from a URL
          return getImagesFromURL(
              numberOfCrawlers,
              maxDepthOfCrawling,
              maxPagesToFetch,
              url,
              randomAmount
          );
        })
        .collectInto(new ArrayList<String>(), ArrayList::addAll)
        .toFlowable().subscribeOn(Schedulers.io());
  }

  /**
   * Get Images from the URL
   *
   * @param numberOfCrawlers   Number of crawlers to run
   * @param maxDepthOfCrawling Page link depth for crawling
   * @param maxPagesToFetch    Max pages to fetch
   * @param url                Target site top page url
   * @param randomAmount       amount of data to fetch
   * @throws Exception
   */
  protected Flowable<List<String>> getImagesFromURL(
      int numberOfCrawlers, int maxDepthOfCrawling,
      int maxPagesToFetch,
      String url, int randomAmount) {
    return Flowable.just(url).concatMap(u -> {
      List<String> results = exec(
              numberOfCrawlers,
              maxDepthOfCrawling,
              maxPagesToFetch,
              url,
              randomAmount
          );
      return Flowable.just(results);
    }).subscribeOn(Schedulers.io());
  }

  /**
   * Exec Crawling
   *
   * @param numberOfCrawlers
   * @param maxDepthOfCrawling
   * @param maxPagesToFetch
   * @param domain
   * @param randomAmount
   * @throws Exception
   */
  public List<String> exec(
      int numberOfCrawlers, int maxDepthOfCrawling, int maxPagesToFetch,
      String domain, int randomAmount) throws Exception {

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

    PageFetcher pageFetcher = new PageFetcher(config);
    RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
    RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
    CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

    controller.addSeed(domain);

    ImageCrawler.configure(domain, randomAmount);

    //Start crawling
    controller.startNonBlocking(ImageCrawler.class, numberOfCrawlers);

    controller.waitUntilFinish();

    // Correcting URLs from each crawlers' results.
    List<Object> crawlersLocalData = controller.getCrawlersLocalData();
    List<String> gatheredURLs = new ArrayList<>();
    
    for (Object localData : crawlersLocalData) {
      @SuppressWarnings("unchecked")
      List<String> urlLists = (List<String>) (localData);
      gatheredURLs.addAll(urlLists);
    }
    return gatheredURLs;
  }

  /**
   * Create Return json value
   *
   * @param resourceRequest
   * @param resourceResponse
   * @param urls             URL string list
   * @return json URL strings
   */
  protected JSONObject createReturnJson(ResourceRequest resourceRequest,
      ResourceResponse resourceResponse, List<String> urls) {

    JSONObject rootJSONObject = JSONFactoryUtil.createJSONObject();

    //Add plain text with line breaks for textarea
    rootJSONObject.put("urlstr", String.join(LDFPortletKeys.EOL, urls));

    JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

    for (String url : urls) {
      JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();

      curUserJSONObject.put("url", url);
      jsonArray.put(curUserJSONObject);
    }
    rootJSONObject.put("urllist", jsonArray);

    return rootJSONObject;
  }

}
