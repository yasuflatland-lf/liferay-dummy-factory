package com.liferay.support.tools.portlet.actions;

import com.google.common.collect.Maps;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.util.JournalConverter;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.CommonUtil;
import com.liferay.support.tools.utils.DDMLocalUtil;
import com.liferay.support.tools.utils.ProgressManager;
import com.liferay.support.tools.utils.RandomizeContentGenerator;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Create Web Contents
 * 
 * @author Yasuyuki Takeo
 */
@Component(
    immediate = true, 
    property = { 
        "javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
        "mvc.command.name=" + LDFPortletKeys.WCM
    }, 
    service = MVCActionCommand.class
)
public class JournalMVCActionCommand extends BaseMVCActionCommand {

	/**
	 * Create Web Contents
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception
	 */
	private void createJournals(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long numberOfArticles = 0;
		String baseTitle = "";
		String baseArticle = "";
		long[] groupIds;
		long folderId = 0;
		String[] locales;
		Boolean fakeContentsGenerateEnable;
		int totalParagraphs;
		int titleWords;
		int randomAmount;
		String linkLists;

		// Fetch data
		numberOfArticles = ParamUtil.getLong(actionRequest, "numberOfArticles", 1);
		baseTitle = ParamUtil.getString(actionRequest, "baseTitle", "");
		baseArticle = ParamUtil.getString(actionRequest, "baseArticle", "");
		folderId = ParamUtil.getLong(actionRequest, "folderId", 0);
		totalParagraphs = ParamUtil.getInteger(actionRequest, "totalParagraphs", 0);
		titleWords = ParamUtil.getInteger(actionRequest, "titleWords", 0);
		randomAmount = ParamUtil.getInteger(actionRequest, "randomAmount", 0);
		fakeContentsGenerateEnable = ParamUtil.getBoolean(actionRequest, "fakeContentsGenerateEnable", false);
		linkLists = ParamUtil.getString(actionRequest, "linkLists", "");

		// Locales
		String[] defLang = { LocaleUtil.getDefault().toString() };
		locales = ParamUtil.getStringValues(actionRequest, "locales", defLang);

		// Sites
		String[] groupsStrIds = ParamUtil.getStringValues(actionRequest, "groupIds",
				new String[] { String.valueOf(themeDisplay.getScopeGroupId()) });
		groupIds = _commonUtil.convertStringToLongArray(groupsStrIds);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(Group.class.getName(), actionRequest);

		// Fetch default locale
		Locale defaultLocale = LocaleUtil.fromLanguageId(themeDisplay.getUser().getLanguageId());

		Map<Locale, String> descriptionMap = new ConcurrentHashMap<Locale, String>();
		descriptionMap.put(defaultLocale, StringPool.BLANK);

		// Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(actionRequest);

		for (long groupId : groupIds) {
			System.out.println("Starting to create " + numberOfArticles + " articles for site id <"
					+ _groupLocalServiceUtil.getGroup(groupId).getDescriptiveName() + ">");

			for (long i = 1; i <= numberOfArticles; i++) {
				// Update progress
				progressManager.trackProgress(i, numberOfArticles);

				// Build title
				StringBundler title = new StringBundler(2);
				baseTitle = (fakeContentsGenerateEnable) 
					? _randomizeContentGenerator.generateRandomTitleString(defaultLocale.getLanguage(), titleWords)
					: baseTitle;
				
				title.append(baseTitle);

				// Add number more then one article
				if (1 < numberOfArticles) {
					title.append(i);
				}

				// Build contents
				baseArticle = (fakeContentsGenerateEnable) 
						? _randomizeContentGenerator.generateRandomContents(defaultLocale.getLanguage(), totalParagraphs, randomAmount, linkLists)
						: baseArticle;
				String content = buildFields(themeDisplay.getCompanyGroupId(), locales, baseArticle);
				
				Map<Locale, String> titleMap = new ConcurrentHashMap<Locale, String>();
				titleMap.put(defaultLocale, title.toString());

				try {
					// Create article
					_journalArticleLocalService.addArticle(serviceContext.getUserId(), // userId,
							groupId, // groupId,
							folderId, // folderId
							titleMap, // titleMap
							descriptionMap, // descriptionMap
							content, // content
							LDFPortletKeys._DDM_STRUCTURE_KEY, // ddmStructureKey,
							LDFPortletKeys._DDM_TEMPLATE_KEY, // ddmTemplateKey,
							serviceContext // serviceContext
					);
				} catch (Exception e) {
					// Finish progress
					progressManager.finish();
					throw e;
				}
			}
		}

		// Finish progress
		progressManager.finish();

		SessionMessages.add(actionRequest, "success");

		System.out.println("Finished creating " + numberOfArticles + " articles");
	}
	
	/**
	 * Build content field
	 * 
	 * @param groupId Company group ID
	 * @param locales locales
	 * @param baseArticle content data
	 * @return DDMStructure applied content XML strings
	 * @throws Exception
	 */
	protected String buildFields(long groupId, String[] locales, String baseArticle) throws Exception {
		DDMStructure ddmStructure = _DDMStructureLocalService.getStructure(groupId,
				PortalUtil.getClassNameId(JournalArticle.class), LDFPortletKeys._DDM_STRUCTURE_KEY);

		Map<String, Serializable> fieldsMap = Maps.newConcurrentMap();
		fieldsMap.put(DDM_CONTENT, baseArticle);

		Fields fields = _ddmLocalUtil.toFields(ddmStructure.getStructureId(), fieldsMap, locales,
				LocaleUtil.getDefault());

		return _journalConverter.getContent(ddmStructure, fields);
	}

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {

		try {
			// Create Web Contents
			createJournals(actionRequest, actionResponse);

		} catch (Exception e) {
			hideDefaultSuccessMessage(actionRequest);
			_log.error(e, e);
		}

		actionResponse.setRenderParameter("mvcRenderCommandName", LDFPortletKeys.COMMON);
	}

	@Reference
	private JournalConverter _journalConverter;
	
	@Reference
	private JournalArticleLocalService _journalArticleLocalService;
	
	@Reference
	private GroupLocalService _groupLocalServiceUtil;
	
	@Reference
	private DDMStructureLocalService _DDMStructureLocalService;
	
	@Reference
	private DDMLocalUtil _ddmLocalUtil;
	
	@Reference
	private CommonUtil _commonUtil;
	
	@Reference
	private RandomizeContentGenerator _randomizeContentGenerator;

	private static final String DDM_CONTENT = "content";

	private static final Log _log = LogFactoryUtil.getLog(JournalMVCActionCommand.class);
}
