package com.liferay.support.tools.wiki;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.settings.GroupServiceSettingsLocator;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;
import com.liferay.wiki.configuration.WikiGroupServiceOverriddenConfiguration;
import com.liferay.wiki.constants.WikiConstants;
import com.liferay.wiki.model.WikiNode;
import com.liferay.wiki.model.WikiPage;
import com.liferay.wiki.model.WikiPageConstants;
import com.liferay.wiki.service.WikiNodeLocalService;
import com.liferay.wiki.service.WikiPageLocalService;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = WikiPageDummyGenerator.class)
public class WikiPageDummyGenerator extends DummyGenerator<WikiContext> {

	@Override
	protected WikiContext getContext(ActionRequest request) throws Exception {
		return new WikiContext(request);
	}

	/**
	 * Get Default format for Wiki
	 * 
	 * @param paramContext
	 * @return default format of the selected node.
	 * @throws PortalException
	 */
	protected String getDefaultFormat(WikiContext paramContext) throws PortalException {
		
		String format = paramContext.getFormat();
		
		if(!format.isEmpty()) {
			return format;
		}
		
		WikiNode node = _wikiNodeLocalService.getNode(paramContext.getNodeId());
		
		WikiGroupServiceOverriddenConfiguration
		wikiGroupServiceOverriddenConfiguration =
			configurationProvider.getConfiguration(
				WikiGroupServiceOverriddenConfiguration.class,
				new GroupServiceSettingsLocator(
					node.getGroupId(), WikiConstants.SERVICE_NAME));

		format = wikiGroupServiceOverriddenConfiguration.defaultFormat();
		
		if(_log.isDebugEnabled()) {
			_log.debug("Default wiki format");
			_log.debug("Node Id : " + String.valueOf(node.getNodeId()));
			_log.debug("Node name : " + node.getName());
		}
		
		return format;
	}
	
	@Override
	protected void exec(ActionRequest request, WikiContext paramContext) throws Exception {
	
		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		System.out.println("Starting to create " + paramContext.getNumberOfpages() + " pages");

		// Get Default format
		String format;
		try {
			format = getDefaultFormat(paramContext);
		} catch (Exception e) {
			//Finish progress
			progressManager.finish();	
			throw e;
		}
		
		for (long i = 1; i <= paramContext.getNumberOfpages(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfpages());
			
			//Create page name
			StringBundler title = new StringBundler(2);
			title.append(paramContext.getBasePageName());
			
			//Create page contents
			StringBundler contents = new StringBundler(2);
			contents.append(paramContext.getBaseContentName());

			//Create page summary
			StringBundler summary = new StringBundler(2);
			summary.append(paramContext.getBaseSummaryName());
			
			//Add number more then one Page
			if(1 < paramContext.getNumberOfpages()) {
				title.append(i);
				contents.append(i);
				summary.append(i);
			}

			WikiPage createdPage;
			
			try {
				if(0 < paramContext.getResourcePrimKey()) {

					// Get parent page
					WikiPage page = _wikiPageLocalService.getPage(paramContext.getResourcePrimKey());
					
					// Generate page as a sub page
					createdPage = _wikiPageLocalService.addPage(
						paramContext.getServiceContext().getUserId(), 
						paramContext.getNodeId(), //noddId
						title.toString(), //title
						WikiPageConstants.VERSION_DEFAULT,
						contents.toString(), //content
						summary.toString(), //summary
						paramContext.isMinorEdit(),//minorEdit
						paramContext.getFormat(),
						true,
						page.getTitle(),
						null,
						paramContext.getServiceContext()); //serviceContext
					
				} else {
					// Generate page as a sub page
					createdPage = _wikiPageLocalService.addPage(
						paramContext.getServiceContext().getUserId(), 
						paramContext.getNodeId(), //noddId
						title.toString(), //title
						WikiPageConstants.VERSION_DEFAULT,
						contents.toString(), //content
						summary.toString(), //summary
						paramContext.isMinorEdit(),//minorEdit
						paramContext.getFormat(),
						true,
						null,
						null,
						paramContext.getServiceContext()); //serviceContext

				}
				
				if(_log.isDebugEnabled()) {
					_log.debug("Created page : " + createdPage.toString());
				}
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
			
		}

		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfpages() + " pages");		
		
	}

	@Reference
	protected WikiPageLocalService _wikiPageLocalService;
	
	@Reference
	protected WikiNodeLocalService _wikiNodeLocalService;	
	
	@Reference
	protected ConfigurationProvider configurationProvider;	
	
	private static final Log _log = LogFactoryUtil.getLog(WikiPageDummyGenerator.class);	
	
}
