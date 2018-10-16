package com.liferay.support.tools.site;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.DuplicateGroupException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.sites.kernel.util.SitesUtil;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Sites Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = SiteDefaultDummyGenerator.class)
public class SiteDefaultDummyGenerator extends DummyGenerator<SiteContext> {

	@Override
	protected SiteContext getContext(ActionRequest request) {

		return new SiteContext(request);
	}

	@Override
	protected void exec(ActionRequest request, SiteContext paramContext) throws Exception {

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		System.out.println("Starting to create " + paramContext.getNumberOfSites() + " sites");

		@SuppressWarnings("serial")
		Map<Locale, String> descriptionMap = new ConcurrentHashMap<Locale, String>() {
			{put(LocaleUtil.getDefault(), StringPool.BLANK);}
		};		
		
		for (long i = 1; i <= paramContext.getNumberOfSites(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfSites());

			//Create Site Name
			StringBundler siteName = new StringBundler(2);
			siteName.append(paramContext.getBaseSiteName());

			//Add number more then one site
			if(1 < paramContext.getNumberOfSites()) {
				siteName.append(i);
			}
			
			@SuppressWarnings("serial")
			Map<Locale, String> nameMap = new ConcurrentHashMap<Locale, String>() {
				{put(LocaleUtil.getDefault(), siteName.toString());}
			};
			
			try {
				
				Group liveGroup = _groupLocalService.addGroup(
					paramContext.getServiceContext().getUserId(), //userId
					paramContext.getParentGroupId(), // parentGroupId
					null, // className
					0, //classPK
					paramContext.getLiveGroupId(), //liveGroupId
					nameMap, // nameMap
					descriptionMap, // descriptionMap
					paramContext.getSiteType(), //type
					paramContext.isManualMembership(), //manualMembership
					GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION, // membershipRestriction
					StringPool.BLANK, //friendlyURL
					paramContext.isSite(), //site
					paramContext.isInheritContent(), //inheritContent
					paramContext.isActive(), //active
					paramContext.getServiceContext()); //serviceContext
				
				// Set Site Template if it's selected.
				SitesUtil.updateLayoutSetPrototypesLinks(
					liveGroup, 
					paramContext.getPublicLayoutSetPrototypeId(),
					0,
					paramContext.isPublicLayoutSetPrototypeLinkEnabled(),
					false);				
				
			} catch (Exception e) {
				if (e instanceof DuplicateGroupException ) {
					_log.error("Site <" + siteName.toString() + "> is duplicated. Skip : " + e.getMessage());
				}
				else {
					//Finish progress
					progressManager.finish();	
					throw e;
				}
			}
		}

		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfSites() + " sites");

	}

	@Reference
	private GroupLocalService _groupLocalService;	
	
	private static final Log _log = LogFactoryUtil.getLog(
			SiteDefaultDummyGenerator.class);		

}
