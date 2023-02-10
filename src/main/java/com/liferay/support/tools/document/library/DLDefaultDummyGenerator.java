package com.liferay.support.tools.document.library;

import com.google.common.collect.Lists;
import com.liferay.document.library.kernel.exception.DuplicateFileEntryException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.dynamic.data.mapping.form.values.factory.DDMFormValuesFactory;
import com.liferay.dynamic.data.mapping.kernel.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldOptions;
import com.liferay.dynamic.data.mapping.model.LocalizedValue;
import com.liferay.dynamic.data.mapping.model.Value;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.DDMBeanTranslator;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.RandomUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.DynamicServletRequest;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TempFileEntryUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.common.ParamContext;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.portlet.ActionRequest;
import javax.servlet.http.HttpServletRequest;

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

				ServiceContext serviceContext = _createServiceContext(paramContext);
				if(paramContext.getTempFileEntries().size() == 0) {
					// https://github.com/liferay/liferay-portal-ee/blob/7.3.x/modules/apps/document-library/document-library-web/src/main/java/com/liferay/document/library/web/internal/portlet/action/EditFileEntryMVCActionCommand.java#L992


					_dLAppLocalService.addFileEntry(
						serviceContext.getUserId(), //userId,
						paramContext.getGroupId(), // repositoryId,
						paramContext.getFolderId(), // folderId,
						sourceFileName.toString(), //sourceFileName,
						ContentTypes.APPLICATION_OCTET_STREAM, //mimeType,
						title.toString(), //title,
						paramContext.getBaseDocumentDescription(), //description,
						StringPool.BLANK, //changeLog,
						dummyFile, //file,
							serviceContext);

				} else {
					FileEntry tf = paramContext.getRandomFileEntry();
					String fileName = title.toString() + StringPool.PERIOD + tf.getExtension();

					_dLAppLocalService.addFileEntry(
						serviceContext.getUserId(), //userId,
						paramContext.getGroupId(), // repositoryId,
						paramContext.getFolderId(), // folderId,
						fileName, //sourceFileName,
						tf.getMimeType(), //mimeType,
						fileName, //title,
						paramContext.getBaseDocumentDescription(), //description,
						StringPool.BLANK, //changeLog,
						tf.getContentStream(), //file,
						tf.getSize(),
							serviceContext);
				}

			} catch (Exception e) {

				if(e instanceof DuplicateFileEntryException) {
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

	private void _setUpDDMFormValues(ServiceContext serviceContext)
			throws PortalException {

		long fileEntryTypeId = ParamUtil.getLong(
				serviceContext, "fileEntryTypeId", -1);
		String baseDocumentFields = ParamUtil.getString(
				serviceContext, "baseDocumentFields", StringPool.BLANK);

		List<List<String>> baseDocumentFieldOptions = new ArrayList<>();

		if (!StringPool.BLANK.equals(baseDocumentFields)) {
			List<String> fieldsOptionsList = Arrays.asList(StringUtil.split(baseDocumentFields, ';'));
			for (String fieldsOptions: fieldsOptionsList) {
				ArrayList<String> options = new ArrayList<>();
				baseDocumentFieldOptions.add(options);
				options.addAll(com.liferay.petra.string.StringUtil.split(fieldsOptions, '|'));
			}
		}

		if (fileEntryTypeId == -1) {
			return;
		}

		DLFileEntryType dlFileEntryType =
				_dlFileEntryTypeLocalService.getDLFileEntryType(fileEntryTypeId);

		for (DDMStructure ddmStructure : dlFileEntryType.getDDMStructures()) {
			String className =
					com.liferay.dynamic.data.mapping.kernel.DDMFormValues.class.
							getName();

			DDMFormValues ddmFormValues = _ddmFormValuesFactory.create(
					_getDDMStructureHttpServletRequest(
							serviceContext.getRequest(), ddmStructure.getStructureId()),
					_ddmBeanTranslator.translate(ddmStructure.getDDMForm()));

			if (!baseDocumentFieldOptions.isEmpty()) {
				int index = 0;
				int size = baseDocumentFieldOptions.size();

				for (DDMFormFieldValue ddmFormFieldValue : ddmFormValues.getDDMFormFieldValues()) {
					Map<Locale, String> values = ddmFormFieldValue.getValue().getValues();
					if (index < size) {
						for (Locale key : values.keySet()) {
							List<String> options = baseDocumentFieldOptions.get(index);
							String value = options.get(RandomUtil.nextInt(options.size())).trim();
							DDMFormField ddmFormField = ddmFormFieldValue.getDDMFormField();
							if ("select".equals(ddmFormField.getType())) {
								DDMFormFieldOptions fieldOptions = ddmFormField.getDDMFormFieldOptions();
								Map<String, LocalizedValue> fieldOptionsMap = fieldOptions.getOptions();
								if (fieldOptionsMap.size() > 0) {
									List<String> entries = fieldOptionsMap.values().stream().map(localizedValue -> localizedValue.getString(fieldOptions.getDefaultLocale())).collect(Collectors.toList());
									if ("?".equals(value)) {
										value = entries.get(RandomUtil.nextInt(entries.size()));
									} else {
										if (!entries.contains(value)) {
											_log.error("** Unable to use the value: " + value);
											_log.error("** Valid values: " + Arrays.toString(entries.toArray()));
										}
									}
								}
							}
							values.replace(key, value);
						}
					} else {
						break;
					}
					index++;
				}
			}

			serviceContext.setAttribute(
					className + StringPool.POUND + ddmStructure.getStructureId(),
					_ddmBeanTranslator.translate(ddmFormValues));
		}
	}

	private HttpServletRequest _getDDMStructureHttpServletRequest(
			HttpServletRequest httpServletRequest, long structureId) {

		DynamicServletRequest dynamicServletRequest = new DynamicServletRequest(
				httpServletRequest, new HashMap<>());

		String namespace = String.valueOf(structureId) + StringPool.UNDERLINE;

		Map<String, String[]> parameterMap =
				httpServletRequest.getParameterMap();

		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String parameterName = entry.getKey();

			if (StringUtil.startsWith(parameterName, namespace)) {
				dynamicServletRequest.setParameterValues(
						parameterName.substring(namespace.length()),
						entry.getValue());
			}
		}

		return dynamicServletRequest;
	}
	private ServiceContext _createServiceContext(
			DLContext paramContext)
			throws PortalException {


		ServiceContext serviceContext = paramContext.getServiceContext();

		if (serviceContext == null) {
			serviceContext = new ServiceContext();
			paramContext.setServiceContext(serviceContext);
		}
		serviceContext.setAttribute("baseDocumentFields", paramContext.getBaseDocumentFields());

		_setUpDDMFormValues(serviceContext);

		return serviceContext;
	}

	/**
	 * Delete All temp file entries
	 *
	 * @param tempFileEntries
	 * @throws PortalException
	 */
	protected void deleteAllTempFileEntries(List<FileEntry> tempFileEntries) throws PortalException {

		for(FileEntry fileEntry : tempFileEntries) {
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

		for(DLFolder df : dlFolders) {

			List<DLFileEntry> dlFileEntries =
				_dlFileEntryLocalService.getFileEntries(
						df.getGroupId(), df.getFolderId());

			for(DLFileEntry fileEntry : dlFileEntries) {
				TempFileEntryUtil.deleteTempFileEntry(fileEntry.getFileEntryId());
			}
		}
	}

	@Reference
	private DDMBeanTranslator _ddmBeanTranslator;
	@Reference
	private DDMFormValuesFactory _ddmFormValuesFactory;
	@Reference
	private DLAppLocalService _dLAppLocalService;
	@Reference
	private DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;
	@Reference
	private DLFolderLocalService _dlFolderLocalService;

	@Reference
	private DLFileEntryLocalService _dlFileEntryLocalService;

	private static final Log _log = LogFactoryUtil.getLog(DLDefaultDummyGenerator.class);

}
