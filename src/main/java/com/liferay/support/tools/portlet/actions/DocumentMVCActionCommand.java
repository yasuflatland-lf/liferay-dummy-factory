package com.liferay.support.tools.portlet.actions;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Documents
 * 
 * @author Yasuyuki Takeo
 */
@Component(
    immediate = true, 
    property = { 
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
        "mvc.command.name=" + LDFPortletKeys.DOCUMENTS
    }, 
    service = MVCActionCommand.class
)
public class DocumentMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Create Documents
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws PortalException 
	 */
	private void createDocuments(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		long numberOfDocuments = 0;
		String baseDocumentTitle = "";
		String baseDocumentDescription = "";
		long groupId = 0;
		long folderId = 0;  
		
		// Fetch data
		numberOfDocuments = ParamUtil.getLong(actionRequest, "numberOfDocuments", 1);
		baseDocumentTitle = ParamUtil.getString(actionRequest, "baseDocumentTitle", "");
		baseDocumentDescription = ParamUtil.getString(actionRequest, "baseDocumentDescription", "");
		folderId = ParamUtil.getLong(actionRequest, "folderId", 0);
		
		// Sites
		groupId = ParamUtil.getLong(actionRequest, "groupId", themeDisplay.getScopeGroupId());
		
		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Group.class.getName(), actionRequest);				

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(actionRequest, 0);
		
		System.out.println("Starting to create " + numberOfDocuments + " documents");

		for (long i = 1; i <= numberOfDocuments; i++) {
			//Update progress
			progressManager.trackProgress(i, numberOfDocuments);
			
			StringBundler title = new StringBundler(2);
			title.append(baseDocumentTitle);

			//Add number more then one docunent
			if(1 < numberOfDocuments) {
				title.append(i);
			}

			StringBundler sourceFileName = new StringBundler(2);
			sourceFileName.append(title.toString());
			sourceFileName.append(".txt");

			byte[] dummyFile = new byte[0];
			
			try {
				
				_dLAppLocalService.addFileEntry(serviceContext.getUserId(), //userId, 
					groupId, // repositoryId,
					folderId, // folderId,
					sourceFileName.toString(), //sourceFileName, 
					ContentTypes.APPLICATION_OCTET_STREAM, //mimeType, 
					title.toString(), //title, 
					baseDocumentDescription, //description,
					StringPool.BLANK, //changeLog, 
					dummyFile, //file,
					serviceContext);
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
		}

		//Finish progress
		progressManager.finish();	

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfDocuments + " documents");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {
		
		try {
			// Create Documents
			createDocuments(actionRequest, actionResponse);
		} catch (Exception e) {
			hideDefaultSuccessMessage(actionRequest);
			_log.error(e,e);
		}

		actionResponse.setRenderParameter(
				"mvcRenderCommandName", LDFPortletKeys.COMMON);		
	}


	@Reference
	private DLAppLocalService _dLAppLocalService;

	
	private static final Log _log = LogFactoryUtil.getLog(DocumentMVCActionCommand.class);	
}
