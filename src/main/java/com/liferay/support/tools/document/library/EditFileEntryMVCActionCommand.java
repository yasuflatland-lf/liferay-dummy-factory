package com.liferay.support.tools.document.library;

import com.liferay.asset.kernel.exception.AssetCategoryException;
import com.liferay.asset.kernel.exception.AssetTagException;
import com.liferay.document.library.kernel.antivirus.AntivirusScannerException;
import com.liferay.document.library.kernel.exception.DuplicateFileEntryException;
import com.liferay.document.library.kernel.exception.DuplicateFolderNameException;
import com.liferay.document.library.kernel.exception.FileEntryLockException;
import com.liferay.document.library.kernel.exception.FileExtensionException;
import com.liferay.document.library.kernel.exception.FileMimeTypeException;
import com.liferay.document.library.kernel.exception.FileNameException;
import com.liferay.document.library.kernel.exception.FileSizeException;
import com.liferay.document.library.kernel.exception.InvalidFileVersionException;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.exception.NoSuchFolderException;
import com.liferay.document.library.kernel.exception.SourceFileNameException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLTrashService;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.dynamic.data.mapping.kernel.StorageFieldRequiredException;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.lock.DuplicateLockException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.TrashedModel;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.repository.capabilities.TrashCapability;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.ServletResponseConstants;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.settings.PortletInstanceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsFactoryUtil;
import com.liferay.portal.kernel.settings.TypedSettings;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.LiferayFileItemException;
import com.liferay.portal.kernel.upload.UploadException;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.upload.UploadRequestSizeException;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TempFileEntryUtil;
import com.liferay.portal.kernel.util.TextFormatter;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.trash.kernel.util.TrashUtil;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/df/document/edit_file_entry",
		"mvc.command.name=/df/document/upload_multiple_file_entries"
	},
	service = MVCActionCommand.class
)
public class EditFileEntryMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);
		
		FileEntry fileEntry = null;

		PortletConfig portletConfig = getPortletConfig(actionRequest);

		try {
			UploadException uploadException = (UploadException) actionRequest.getAttribute(WebKeys.UPLOAD_EXCEPTION);

			if (uploadException != null) {
				Throwable cause = uploadException.getCause();

				if (cmd.equals(Constants.ADD_TEMP)) {
					if (cause instanceof FileUploadBase.IOFileUploadException) {
						if (_log.isInfoEnabled()) {
							_log.info("Temporary upload was cancelled");
						}
					}
				} else {
					if (uploadException.isExceededFileSizeLimit()) {
						throw new FileSizeException(cause);
					}

					if (uploadException.isExceededLiferayFileItemSizeLimit()) {
						throw new LiferayFileItemException(cause);
					}

					if (uploadException.isExceededUploadRequestSizeLimit()) {
						throw new UploadRequestSizeException(cause);
					}

					throw new PortalException(cause);
				}
			} else if (cmd.equals(Constants.ADD)) {

				fileEntry = updateFileEntry(portletConfig, actionRequest, actionResponse);
			} else if (cmd.equals(Constants.ADD_TEMP)) {
				addTempFileEntry(actionRequest, actionResponse);
			} else if (cmd.equals(Constants.DELETE)) {
				deleteFileEntry(actionRequest, false);
			} else if (cmd.equals(Constants.DELETE_TEMP)) {
				deleteTempFileEntry(actionRequest, actionResponse);
			} 

			if (cmd.equals(Constants.ADD_TEMP) 		|| 
				cmd.equals(Constants.DELETE_TEMP)) {
				actionResponse.setRenderParameter("mvcPath", "/null.jsp");
			} else {
				String redirect = ParamUtil.getString(actionRequest, "redirect");
				int workflowAction = ParamUtil.getInteger(actionRequest, "workflowAction",
						WorkflowConstants.ACTION_SAVE_DRAFT);

				if ((fileEntry != null) && (workflowAction == WorkflowConstants.ACTION_SAVE_DRAFT)) {

					redirect = getSaveAndContinueRedirect(portletConfig, actionRequest, fileEntry, redirect);

					sendRedirect(actionRequest, actionResponse, redirect);
				}
			}
		} catch (Exception e) {
			handleUploadException(portletConfig, actionRequest, actionResponse, cmd, e);
		}
	}

	/**
	 * Managing redirect after saving FileEntry
	 * 
	 * @param portletConfig
	 * @param actionRequest
	 * @param fileEntry
	 * @param redirect
	 * @return
	 * @throws Exception
	 */
	protected String getSaveAndContinueRedirect(
			PortletConfig portletConfig, ActionRequest actionRequest,
			FileEntry fileEntry, String redirect)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		LiferayPortletURL portletURL = PortletURLFactoryUtil.create(
			actionRequest, portletConfig.getPortletName(), themeDisplay.getLayout(),
			PortletRequest.RENDER_PHASE);

		portletURL.setParameter(
			"mvcRenderCommandName", "/df/document/edit_file_entry");
		portletURL.setParameter(Constants.CMD, Constants.UPDATE, false);
		portletURL.setParameter("redirect", redirect, false);
		portletURL.setParameter(
			"groupId", String.valueOf(fileEntry.getGroupId()), false);
		portletURL.setParameter(
			"fileEntryId", String.valueOf(fileEntry.getFileEntryId()), false);
		portletURL.setParameter(
			"version", String.valueOf(fileEntry.getVersion()), false);
		portletURL.setWindowState(actionRequest.getWindowState());

		return portletURL.toString();
	}	
	

	
	/**
	 * Delete Temp file entry
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception
	 */
	protected void deleteTempFileEntry(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long folderId = ParamUtil.getLong(actionRequest, "folderId");
		String fileName = ParamUtil.getString(actionRequest, "fileName");

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		try {
			_dlAppService.deleteTempFileEntry(themeDisplay.getScopeGroupId(), folderId, TEMP_FOLDER_NAME, fileName);

			jsonObject.put("deleted", Boolean.TRUE);
		} catch (Exception e) {
			String errorMessage = themeDisplay.translate("an-unexpected-error-occurred-while-deleting-the-file");

			jsonObject.put("deleted", Boolean.FALSE);
			jsonObject.put("errorMessage", errorMessage);
		}

		JSONPortletResponseUtil.writeJSON(actionRequest, actionResponse, jsonObject);
	}

	/**
	 * Delete File entry
	 * 
	 * @param actionRequest
	 * @param moveToTrash
	 * @throws Exception
	 */
	protected void deleteFileEntry(ActionRequest actionRequest, boolean moveToTrash) throws Exception {

		long fileEntryId = ParamUtil.getLong(actionRequest, "fileEntryId");

		if (fileEntryId == 0) {
			return;
		}

		String version = ParamUtil.getString(actionRequest, "version");

		if (Validator.isNotNull(version)) {
			_dlAppService.deleteFileVersion(fileEntryId, version);

			return;
		}

		if (!moveToTrash) {
			_dlAppService.deleteFileEntry(fileEntryId);

			return;
		}

		FileEntry fileEntry = _dlAppService.getFileEntry(fileEntryId);

		if (fileEntry.isRepositoryCapabilityProvided(TrashCapability.class)) {
			fileEntry = _dlTrashService.moveFileEntryToTrash(fileEntryId);

			TrashUtil.addTrashSessionMessages(actionRequest, (TrashedModel) fileEntry.getModel());
		}

		hideDefaultSuccessMessage(actionRequest);
	}

	/**
	 * Add temp file entry
	 * 
	 * While creating a journal document, files are stored in the temporaly
	 * repository until the journal article is actually created.
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception
	 */
	protected void addTempFileEntry(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(actionRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long folderId = ParamUtil.getLong(uploadPortletRequest, "folderId");
		String sourceFileName = uploadPortletRequest.getFileName("file");

		InputStream inputStream = null;

		try {
			String tempFileName = TempFileEntryUtil.getTempFileName(sourceFileName);

			inputStream = uploadPortletRequest.getFileAsStream("file");

			String mimeType = uploadPortletRequest.getContentType("file");

			FileEntry fileEntry = _dlAppService.addTempFileEntry(themeDisplay.getScopeGroupId(), folderId,
					TEMP_FOLDER_NAME, tempFileName, inputStream, mimeType);

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

			jsonObject.put("groupId", fileEntry.getGroupId());
			jsonObject.put("name", fileEntry.getTitle());
			jsonObject.put("title", sourceFileName);
			jsonObject.put("uuid", fileEntry.getUuid());

			JSONPortletResponseUtil.writeJSON(actionRequest, actionResponse, jsonObject);
		} finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	/**
	 * File Entry Update
	 * 
	 * @param portletConfig
	 * @param actionRequest
	 * @param actionResponse
	 * @return
	 * @throws Exception
	 */
	public FileEntry updateFileEntry(PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse) throws Exception {

		UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(actionRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		String cmd = ParamUtil.getString(uploadPortletRequest, Constants.CMD);

		long fileEntryId = ParamUtil.getLong(uploadPortletRequest, "fileEntryId");

		long repositoryId = ParamUtil.getLong(uploadPortletRequest, "repositoryId");
		long folderId = ParamUtil.getLong(uploadPortletRequest, "folderId");
		String sourceFileName = uploadPortletRequest.getFileName("file");
		String title = ParamUtil.getString(uploadPortletRequest, "title");
		String description = ParamUtil.getString(uploadPortletRequest, "description");
		String changeLog = ParamUtil.getString(uploadPortletRequest, "changeLog");
		boolean majorVersion = ParamUtil.getBoolean(uploadPortletRequest, "majorVersion");

		if (folderId > 0) {
			Folder folder = _dlAppService.getFolder(folderId);

			if (folder.getGroupId() != themeDisplay.getScopeGroupId()) {
				throw new NoSuchFolderException("{folderId=" + folderId + "}");
			}
		}

		InputStream inputStream = null;

		try {
			String contentType = uploadPortletRequest.getContentType("file");
			long size = uploadPortletRequest.getSize("file");

			if ((cmd.equals(Constants.ADD)) && (size == 0)) {

				contentType = MimeTypesUtil.getContentType(title);
			}

			inputStream = uploadPortletRequest.getFileAsStream("file");

			ServiceContext serviceContext = ServiceContextFactory.getInstance(DLFileEntry.class.getName(),
					uploadPortletRequest);

			FileEntry fileEntry = null;

			if (cmd.equals(Constants.ADD)) {

				// Add file entry
				fileEntry = _dlAppService.addFileEntry(repositoryId, folderId, sourceFileName, contentType, title,
						description, changeLog, inputStream, size, serviceContext);

			} else {

				// Update file entry
				fileEntry = _dlAppService.updateFileEntry(fileEntryId, sourceFileName, contentType, title, description,
						changeLog, majorVersion, inputStream, size, serviceContext);
			}

			return fileEntry;
		} finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	/**
	 * Exception Handler
	 * 
	 * @param portletConfig
	 * @param actionRequest
	 * @param actionResponse
	 * @param cmd
	 * @param e
	 * @throws Exception
	 */
	protected void handleUploadException(PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse, String cmd, Exception e) throws Exception {

		if (e instanceof AssetCategoryException || e instanceof AssetTagException) {

			SessionErrors.add(actionRequest, e.getClass(), e);
		} else if (e instanceof AntivirusScannerException || e instanceof DuplicateFileEntryException
				|| e instanceof DuplicateFolderNameException || e instanceof FileExtensionException
				|| e instanceof FileMimeTypeException || e instanceof FileNameException
				|| e instanceof FileSizeException || e instanceof LiferayFileItemException
				|| e instanceof NoSuchFolderException || e instanceof SourceFileNameException
				|| e instanceof StorageFieldRequiredException || e instanceof UploadRequestSizeException) {

			if (!cmd.equals(Constants.ADD_DYNAMIC) && !cmd.equals(Constants.ADD_MULTIPLE)
					&& !cmd.equals(Constants.ADD_TEMP)) {

				if (e instanceof AntivirusScannerException) {
					SessionErrors.add(actionRequest, e.getClass(), e);
				} else {
					SessionErrors.add(actionRequest, e.getClass());
				}

				return;
			} else if (cmd.equals(Constants.ADD_TEMP)) {
				hideDefaultErrorMessage(actionRequest);
			}

			if (e instanceof AntivirusScannerException || e instanceof DuplicateFileEntryException
					|| e instanceof FileExtensionException || e instanceof FileNameException
					|| e instanceof FileSizeException || e instanceof UploadRequestSizeException) {

				HttpServletResponse response = PortalUtil.getHttpServletResponse(actionResponse);

				response.setContentType(ContentTypes.TEXT_HTML);
				response.setStatus(HttpServletResponse.SC_OK);

				String errorMessage = StringPool.BLANK;
				int errorType = 0;

				ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

				if (e instanceof AntivirusScannerException) {
					AntivirusScannerException ase = (AntivirusScannerException) e;

					errorMessage = themeDisplay.translate(ase.getMessageKey());
					errorType = ServletResponseConstants.SC_FILE_ANTIVIRUS_EXCEPTION;
				}

				if (e instanceof DuplicateFileEntryException) {
					errorMessage = themeDisplay.translate("please-enter-a-unique-document-name");
					errorType = ServletResponseConstants.SC_DUPLICATE_FILE_EXCEPTION;
				} else if (e instanceof FileExtensionException) {
					errorMessage = themeDisplay.translate("please-enter-a-file-with-a-valid-extension-x",
							StringUtil.merge(getAllowedFileExtensions(portletConfig, actionRequest, actionResponse)));
					errorType = ServletResponseConstants.SC_FILE_EXTENSION_EXCEPTION;
				} else if (e instanceof FileNameException) {
					errorMessage = themeDisplay.translate("please-enter-a-file-with-a-valid-file-name");
					errorType = ServletResponseConstants.SC_FILE_NAME_EXCEPTION;
				} else if (e instanceof FileSizeException) {
					long fileMaxSize = PrefsPropsUtil.getLong(PropsKeys.DL_FILE_MAX_SIZE);

					if (fileMaxSize == 0) {
						fileMaxSize = PrefsPropsUtil.getLong(PropsKeys.UPLOAD_SERVLET_REQUEST_IMPL_MAX_SIZE);
					}

					errorMessage = themeDisplay.translate(
							"please-enter-a-file-with-a-valid-file-size-no-larger" + "-than-x",
							TextFormatter.formatStorageSize(fileMaxSize, themeDisplay.getLocale()));

					errorType = ServletResponseConstants.SC_FILE_SIZE_EXCEPTION;
				}

				JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

				jsonObject.put("message", errorMessage);
				jsonObject.put("status", errorType);

				JSONPortletResponseUtil.writeJSON(actionRequest, actionResponse, jsonObject);
			}

			if (e instanceof AntivirusScannerException) {
				SessionErrors.add(actionRequest, e.getClass(), e);
			} else {
				SessionErrors.add(actionRequest, e.getClass());
			}
		} else if (e instanceof DuplicateLockException || e instanceof FileEntryLockException.MustOwnLock
				|| e instanceof InvalidFileVersionException || e instanceof NoSuchFileEntryException
				|| e instanceof PrincipalException) {

			if (e instanceof DuplicateLockException) {
				DuplicateLockException dle = (DuplicateLockException) e;

				SessionErrors.add(actionRequest, dle.getClass(), dle.getLock());
			} else {
				SessionErrors.add(actionRequest, e.getClass());
			}

			actionResponse.setRenderParameter("mvcPath", "/error.jsp");
		} else {
			Throwable cause = e.getCause();

			if (cause instanceof DuplicateFileEntryException) {
				SessionErrors.add(actionRequest, DuplicateFileEntryException.class);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Get Allowed file extensions
	 * 
	 * @param portletConfig
	 * @param portletRequest
	 * @param portletResponse
	 * @return
	 * @throws PortalException
	 */
	protected String[] getAllowedFileExtensions(PortletConfig portletConfig, PortletRequest portletRequest,
			PortletResponse portletResponse) throws PortalException {

		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();

		Settings settings = SettingsFactoryUtil
				.getSettings(new PortletInstanceSettingsLocator(themeDisplay.getLayout(), portletDisplay.getId()));

		TypedSettings typedSettings = new TypedSettings(settings);

		Set<String> extensions = new HashSet<>();

		String[] mimeTypes = typedSettings.getValues("mimeTypes", _MIME_TYPES_DEFAULT);

		for (String mimeType : mimeTypes) {
			extensions.addAll(MimeTypesUtil.getExtensions(mimeType));
		}

		return extensions.toArray(new String[extensions.size()]);
	}

	@Reference(unbind = "-")
	protected void setDLAppService(DLAppService dlAppService) {
		_dlAppService = dlAppService;
	}

	@Reference(unbind = "-")
	protected void setDLTrashService(DLTrashService dlTrashService) {
		_dlTrashService = dlTrashService;
	}

	private DLAppService _dlAppService;
	private DLTrashService _dlTrashService;
	
	public static final String TEMP_FOLDER_NAME = EditFileEntryMVCActionCommand.class.getName() + "_DUMMY_FACTORY";
	
	private static final String[] _MIME_TYPES_DEFAULT = ArrayUtil.toStringArray(DLUtil.getAllMediaGalleryMimeTypes());

	private static final Log _log = LogFactoryUtil.getLog(EditFileEntryMVCActionCommand.class);

}
