package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Sites
 * 
 * @author Yasuyuki Takeo
 */
@Component(
	immediate = true, 
	property = { 
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.SITES
	}, 
	service = MVCActionCommand.class
)
public class SiteMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Create Sites
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException 
	 */
	private void createSites(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException {

		double loader = 10;

		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Group.class.getName(), actionRequest);				

		System.out.println("Starting to create " + numberOfSites + " sites");

		Map<Locale, String> descriptionMap = new HashMap<Locale, String>() {
			{put(LocaleUtil.getDefault(), StringPool.BLANK);}
		};		
		
		for (long i = 1; i <= numberOfSites; i++) {
			if (numberOfSites >= 100) {
				if (i == (int) (numberOfSites * (loader / 100))) {
					System.out.println("Creating sites..." + (int) loader + "% done");
					loader = loader + 10;
				}
			}

			//Create Site Name
			StringBundler siteName = new StringBundler(2);
			siteName.append(baseSiteName).append(i);

			Map<Locale, String> nameMap = new HashMap<Locale, String>() {
				{put(LocaleUtil.getDefault(), siteName.toString());}
			};
			
			_groupLocalService.addGroup(
					serviceContext.getUserId(), //userId
					parentGroupId, // parentGroupId
					null, // className
					0, //classPK
					liveGroupId, //liveGroupId
					nameMap, // nameMap
					descriptionMap, // descriptionMap
					siteType, //type
					manualMembership, //manualMembership
					GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION, // membershipRestriction
					StringPool.BLANK, //friendlyURL
					site, //site
					inheritContent, //inheritContent
					active, //active
					serviceContext); //serviceContext
		}

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfSites + " sites");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		try {
			//Fetch data
			numberOfSites = ParamUtil.getLong(actionRequest, "numberOfSites",1);
			baseSiteName = ParamUtil.getString(actionRequest, "baseSiteName","dummy");
			siteType = ParamUtil.getInteger(actionRequest, "siteType", GroupConstants.TYPE_SITE_OPEN);
			parentGroupId = ParamUtil.getLong(actionRequest, "parentGroupId", GroupConstants.DEFAULT_PARENT_GROUP_ID);
			liveGroupId = ParamUtil.getLong(actionRequest, "liveGroupId", GroupConstants.DEFAULT_LIVE_GROUP_ID);

			manualMembership = ParamUtil.getBoolean(actionRequest, "manualMembership", false);
			site = ParamUtil.getBoolean(actionRequest, "site", true);
			inheritContent = ParamUtil.getBoolean(actionRequest, "inheritContent", false);
			active = ParamUtil.getBoolean(actionRequest, "active", true);

			//Create Sites
			createSites(actionRequest, actionResponse);
		} catch (Throwable e) {
			hideDefaultSuccessMessage(actionRequest);
			e.printStackTrace();
		}
	
	}
	
	@Reference(unbind = "-")
	protected void setGroupLocalService(
			GroupLocalService groupLocalService) {
		_groupLocalService = groupLocalService;
	}

	private GroupLocalService _groupLocalService;	

	private long numberOfSites = 0;
	private String baseSiteName = "";
	private int siteType = GroupConstants.TYPE_SITE_OPEN;
	private long parentGroupId = GroupConstants.DEFAULT_PARENT_GROUP_ID;
	private long liveGroupId = GroupConstants.DEFAULT_LIVE_GROUP_ID;
	private boolean manualMembership = false;
	private boolean site = true;
	private boolean inheritContent = false;
	private boolean active = true;
	
	
}
