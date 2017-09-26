package com.liferay.support.tools.portlet.actions;

import com.google.common.collect.Lists;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ImageCrawlController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.reactivex.Observable;

/**
 * Image Links generators
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/image/list"
	},
	service = MVCResourceCommand.class
)
public class ImageLinksMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws Exception {
		int numberOfCrawlers = ParamUtil.getInteger(resourceRequest, "numberOfCrawlers", -1);
		int maxDepthOfCrawling = ParamUtil.getInteger(resourceRequest, "maxDepthOfCrawling", -1);
		int maxPagesToFetch = ParamUtil.getInteger(resourceRequest, "maxPagesToFetch", -1);
		String tmpUrls = ParamUtil.getString(resourceRequest, "urls", "https://www.shutterstock.com/photos");
		String[] strArray = tmpUrls.split(",");
		List<String> urls = new ArrayList<>(Arrays.asList(strArray));

		if(_log.isDebugEnabled()) {
			_log.debug("numberOfCrawlers : " + String.valueOf(numberOfCrawlers));
			_log.debug("maxDepthOfCrawling : " + String.valueOf(maxDepthOfCrawling));
			_log.debug("maxPagesToFetch : " + String.valueOf(maxPagesToFetch));
		}

		if( numberOfCrawlers >= 0 && 
			maxDepthOfCrawling >= 0 && 
			maxPagesToFetch >= 0 ) {
			
			// Run image links crawler
			run(numberOfCrawlers, maxDepthOfCrawling, maxPagesToFetch, urls);
		}
	}

	/**
	 * Run Image links crawler
	 * 
	 * @param numberOfCrawlers Number of crawlers to run
	 * @param maxDepthOfCrawling Page link depth for crawling
	 * @param maxPagesToFetch Max pages to fetch
	 * @param urls Target site top page urls
	 * @throws Exception
	 */
	public void run(int numberOfCrawlers, int maxDepthOfCrawling, int maxPagesToFetch, List<String> urls)
			throws Exception {

		System.out.println("Image link crawling start");
		
		List<Observable<List<String>>> obsList = Lists.newArrayList();
		for (String url : urls) {
			obsList.add(Observable.<List<String>>create(emitter -> {
				_imageCrawlController.exec(numberOfCrawlers, maxDepthOfCrawling, maxPagesToFetch, url);
				List<String> results = _imageCrawlController.getURL();
				emitter.onNext(results);
				emitter.onComplete();
			}));
		}
		
		System.out.println("Generated crawlers for Store Observable");

		List<String> result = Lists.newArrayList();
		Observable.zip(obsList, (s) -> {
			return s;
		}).subscribe(imgUrls -> {

			@SuppressWarnings("unchecked")
			List<List<String>> orgList = Arrays.asList(imgUrls).stream().map(urlList -> (List<String>) urlList)
					.collect(Collectors.toList());

			List<String> urlStrList = orgList.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
			result.addAll(urlStrList);
		});

		System.out.println("Collected links <" + result + ">");

	}

	@Reference
	private ImageCrawlController _imageCrawlController;

	private static final Log _log = LogFactoryUtil.getLog(UserMVCActionCommand.class);

}
