package com.liferay.support.tools.document.library;

import com.liferay.document.library.kernel.exception.DuplicateFileEntryException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.TempFileEntryUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document Library Generator
 *
 * @author Yasuyuki Takeo
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
            if (1 < paramContext.getNumberOfDocuments()) {
                title.append(i);
            }

            StringBundler sourceFileName = new StringBundler(2);
            sourceFileName.append(title.toString());
            sourceFileName.append(".txt");

            try {

                if (paramContext.getTempFileEntries().size() == 0) {

                    _dLAppLocalService.addFileEntry(
                        null, // externalReferenceCode
                        paramContext.getServiceContext().getUserId(), //userId,
                        paramContext.getGroupId(), // repositoryId,
                        paramContext.getFolderId(), // folderId,
                        sourceFileName.toString(), //sourceFileName,
                        ContentTypes.APPLICATION_OCTET_STREAM, //mimeType,
                        title.toString(), //title,
                        StringPool.BLANK, // urlTitle
                        paramContext.getBaseDocumentDescription(), //description,
                        StringPool.BLANK, //changeLog,
                        new File(System.getProperty("java.io.tmpdir"), "dummy"), //file,
                        (Date)null, // expirationDate
                        (Date)null, // reviewDate
                        paramContext.getServiceContext());

                } else {
                    FileEntry tf = paramContext.getRandomFileEntry();
                    String fileName = title.toString() + StringPool.PERIOD + tf.getExtension();

                    _dLAppLocalService.addFileEntry(
                        null,
                        paramContext.getServiceContext().getUserId(), //userId,
                        paramContext.getGroupId(), // repositoryId,
                        paramContext.getFolderId(), // folderId,
                        fileName, //sourceFileName,
                        tf.getMimeType(), //mimeType,
                        fileName, //title,
                        StringPool.BLANK, // urlTitle
                        paramContext.getBaseDocumentDescription(), //description,
                        StringPool.BLANK, //changeLog,
                        tf.getContentStream(), //file,
                        tf.getSize(),
                        (Date)null, // expirationDate
                        (Date)null, // reviewDate
                        paramContext.getServiceContext());
                }

            } catch (Exception e) {

                if (e instanceof DuplicateFileEntryException) {
                    _log.error(e.getMessage() + " Skip creation.");
                    continue;
                } else {
                    //Finish progress
                    progressManager.finish();
                    throw e;
                }
            }
        }

        // Delete all temp files
        deleteAllTempFileEntries(paramContext.getTempFileEntries());
        deleteAllFilesInFolder(request, paramContext);

        //Finish progress
        progressManager.finish();

        System.out.println("Finished creating " + paramContext.getNumberOfDocuments() + " documents");

    }

    /**
     * Delete All temp file entries
     *
     * @param tempFileEntries
     * @throws PortalException
     */
    protected void deleteAllTempFileEntries(List<FileEntry> tempFileEntries) throws PortalException {

        for (FileEntry fileEntry : tempFileEntries) {
            TempFileEntryUtil.deleteTempFileEntry(fileEntry.getFileEntryId());
        }
    }

    /**
     * Delete all files in a temp folder.
     *
     * @param request
     * @param paramContext
     * @throws PortalException
     */
    protected void deleteAllFilesInFolder(ActionRequest request, DLContext paramContext) throws PortalException {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

        List<DLFolder> dlAllFolders =
            _dlFolderLocalService.getCompanyFolders(
                themeDisplay.getCompanyId(),
                QueryUtil.ALL_POS,
                QueryUtil.ALL_POS);

        List<DLFolder> dlFolders = dlAllFolders.stream()
            .filter(
                df -> df.getName().equals(EditFileEntryMVCActionCommand.TEMP_FOLDER_NAME)
            )
            .collect(Collectors.toList());

        for (DLFolder df : dlFolders) {

            List<DLFileEntry> dlFileEntries =
                _dlFileEntryLocalService.getFileEntries(
                    df.getGroupId(), df.getFolderId());

            for (DLFileEntry fileEntry : dlFileEntries) {
                TempFileEntryUtil.deleteTempFileEntry(fileEntry.getFileEntryId());
            }
        }
    }

    @Reference
    private DLAppLocalService _dLAppLocalService;

    @Reference
    private DLFolderLocalService _dlFolderLocalService;

    @Reference
    private DLFileEntryLocalService _dlFileEntryLocalService;

    private static final Log _log = LogFactoryUtil.getLog(DLDefaultDummyGenerator.class);

}
