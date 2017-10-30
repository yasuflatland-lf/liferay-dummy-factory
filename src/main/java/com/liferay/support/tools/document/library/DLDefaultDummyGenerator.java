package com.liferay.support.tools.document.library;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Document Library Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = DLDefaultDummyGenerator.class)
public class DLDefaultDummyGenerator extends DummyGenerator<DLContext> {

	@Override
	protected DLContext getContext(ActionRequest request) {

		return new DLContext(request);
	}

	@Override
	protected void exec(ActionRequest request, DLContext paramContext) throws PortalException {

		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfDocuments() + " documents");

		for (long i = 1; i <= paramContext.getNumberOfDocuments(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfDocuments());
			
			StringBundler title = new StringBundler(2);
			title.append(paramContext.getBaseDocumentTitle());

			//Add number more then one docunent
			if(1 < paramContext.getNumberOfDocuments()) {
				title.append(i);
			}

			StringBundler sourceFileName = new StringBundler(2);
			sourceFileName.append(title.toString());
			sourceFileName.append(".txt");

			byte[] dummyFile = new byte[0];
			
			try {
				
				_dLAppLocalService.addFileEntry(
					paramContext.getServiceContext().getUserId(), //userId, 
					paramContext.getGroupId(), // repositoryId,
					paramContext.getFolderId(), // folderId,
					sourceFileName.toString(), //sourceFileName, 
					ContentTypes.APPLICATION_OCTET_STREAM, //mimeType, 
					title.toString(), //title, 
					paramContext.getBaseDocumentDescription(), //description,
					StringPool.BLANK, //changeLog, 
					dummyFile, //file,
					paramContext.getServiceContext());
				
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}
		}

		//Finish progress
		progressManager.finish();	

		System.out.println("Finished creating " + paramContext.getNumberOfDocuments() + " documents");

	}

	@Reference
	private DLAppLocalService _dLAppLocalService;

}
