package com.liferay.support.tools.portlet.actions;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
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
	private void createDocuments(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException {

		double loader = 10;

		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Group.class.getName(), actionRequest);				

		System.out.println("Starting to create " + numberOfDocuments + " documents");

		for (long i = 1; i <= numberOfDocuments; i++) {
			if (numberOfDocuments >= 100) {
				if (i == (int) (numberOfDocuments * (loader / 100))) {
					System.out.println("Creating Documents..." + (int) loader + "% done");
					loader = loader + 10;
				}
			}
			
			StringBundler title = new StringBundler(2);
			title.append(baseDocumentTitle);
			title.append(i);

			StringBundler sourceFileName = new StringBundler(2);
			sourceFileName.append(title.toString());
			sourceFileName.append(".txt");

			byte[] dummyFile = new byte[0];
			_dLAppLocalService.addFileEntry(
					serviceContext.getUserId(), //userId, 
					groupId, // repositoryId,
					folderId, // folderId,
					sourceFileName.toString(), //sourceFileName, 
					ContentTypes.APPLICATION_OCTET_STREAM, //mimeType, 
					title.toString(), //title, 
					baseDocumentDescription, //description,
					StringPool.BLANK, //changeLog, 
					dummyFile, //file,
					serviceContext);
			
		}

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfDocuments + " documents");
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {
		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		try {
			// Fetch data
			numberOfDocuments = ParamUtil.getLong(actionRequest, "numberOfDocuments", 1);
			baseDocumentTitle = ParamUtil.getString(actionRequest, "baseDocumentTitle", "");
			baseDocumentDescription = ParamUtil.getString(actionRequest, "baseDocumentDescription", "");
			folderId = ParamUtil.getLong(actionRequest, "folderId", 0);
			
			// Sites
			groupId = ParamUtil.getLong(actionRequest, "groupId", themeDisplay.getScopeGroupId());
			
			// Create Documents
			createDocuments(actionRequest, actionResponse);
		} catch (Throwable e) {
			hideDefaultSuccessMessage(actionRequest);
			e.printStackTrace();
		}

		actionResponse.setRenderParameter(
				"mvcRenderCommandName", LDFPortletKeys.COMMON);		
	}


	@Reference
	private DLAppLocalService _dLAppLocalService;

	private long numberOfDocuments = 0;
	private String baseDocumentTitle = "";
	private String baseDocumentDescription = "";
	private long groupId = 0;
	private long folderId = 0;
}
