package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Pages
 * 
 * @author Yasuyuki Takeo
 */
@Component(
	immediate = true, 
	property = { 
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.PAGES
	}, 
	service = MVCActionCommand.class
)
public class PageMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Create Pages
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException 
	 */
	private void createPages(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		// Parameter values
		long numberOfpages = 0;
		String basePageName = "";
		long groupId = 0;
		long parentLayoutId;
		String layoutType;
		boolean privateLayout;
		boolean hidden;
		
		//Fetch data
		numberOfpages = ParamUtil.getLong(actionRequest, "numberOfpages",0);
		basePageName = ParamUtil.getString(actionRequest, "basePageName","");
		groupId = ParamUtil.getLong(actionRequest, "group",0);
		parentLayoutId = ParamUtil.getLong(actionRequest, "parentLayoutId",LayoutConstants.DEFAULT_PARENT_LAYOUT_ID);
		layoutType = ParamUtil.getString(actionRequest, "layoutType",LayoutConstants.TYPE_PORTLET);
		privateLayout = ParamUtil.getBoolean(actionRequest, "privateLayout", false);
		hidden = ParamUtil.getBoolean(actionRequest, "hidden", false);
		
		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Group.class.getName(), actionRequest);				

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(actionRequest, 0);
		
		System.out.println("Starting to create " + numberOfpages + " pages");

		for (long i = 1; i <= numberOfpages; i++) {
			//Update progress
			progressManager.trackProgress(i, numberOfpages);
			
			//Create page name
			StringBundler name = new StringBundler(2);
			name.append(basePageName);
			
			//Add number more then one Page
			if(1 < numberOfpages) {
				name.append(i);
			}

			try {
				_layoutLocalService.addLayout(serviceContext.getUserId(), groupId, //groupId
					privateLayout, //privateLayout
					parentLayoutId, //parentLayoutId
					name.toString(), //nameMap
					StringPool.BLANK, //titleMap
					StringPool.BLANK, //descriptionMap
					layoutType, //type
					hidden, //hidden
					StringPool.BLANK, //friendlyURL
					serviceContext); //serviceContext
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
			
		}

		//Finish progress
		progressManager.finish();	
		
		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfpages + " pages");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		try {
			//Create pages
			createPages(actionRequest, actionResponse);
			
		} catch (Exception e) {
			hideDefaultSuccessMessage(actionRequest);
			_log.error(e,e);
		}
		
		actionResponse.setRenderParameter(
				"mvcRenderCommandName", LDFPortletKeys.COMMON);		
	}
	
	@Reference
	private LayoutLocalService _layoutLocalService;	

	private static final Log _log = LogFactoryUtil.getLog(PageMVCActionCommand.class);		
}
