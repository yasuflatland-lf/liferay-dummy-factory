package com.liferay.support.tools.portlet.actions;

import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
	 * @throws PortalException 
	 */
	private void createJournals(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		double loader = 10;

		ServiceContext serviceContext = ServiceContextFactory
				.getInstance(Group.class.getName(), actionRequest);				

		Locale defaultLocale = LocaleUtil.fromLanguageId(themeDisplay.getUser().getLanguageId());

		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
		descriptionMap.put(defaultLocale, StringPool.BLANK);
		
		System.out.println("Starting to create " + numberOfArticles + " articles");

		for (long i = 1; i <= numberOfArticles; i++) {
			if (numberOfArticles >= 100) {
				if (i == (int) (numberOfArticles * (loader / 100))) {
					System.out.println("Creating Web Contents..." + (int) loader + "% done");
					loader = loader + 10;
				}
			}
			
			StringBundler title = new StringBundler(2);
			title.append(baseTitle);
			title.append(i);

			Map<Locale, String> titleMap = new HashMap<Locale, String>();
			titleMap.put(defaultLocale, title.toString());

			// Contents
			String content = getContent(baseArticle, locales);
			
			// Create article
			_journalArticleLocalService.addArticle(
					serviceContext.getUserId(), //userId, 
					groupId, //groupId, 
					folderId, //folderId
					titleMap, //titleMap
					descriptionMap, //descriptionMap
					content, // content
					LDFPortletKeys._DDM_STRUCTURE_KEY, //ddmStructureKey, 
					LDFPortletKeys._DDM_TEMPLATE_KEY, //ddmTemplateKey, 
					serviceContext //serviceContext
			);
		}

		SessionMessages.add(actionRequest, "success");
		
		System.out.println("Finished creating " + numberOfArticles + " articles");
	}

	/**
	 * Build content 
	 * 
	 * According to locales, build xml for web contents.
	 * 
	 * @param baseArticle contents
	 * @param locales locales for contents
	 * @return xml for web contents's "contents" parameter.
	 */
	protected String getContent(String baseArticle, String[] locales) {
		
		if(0 == locales.length) {
			locales[0] = LocaleUtil.getDefault().toString();
		}
		
		StringBundler content = new StringBundler();
		content.append("<?xml version=\"1.0\"?>");
		content.append("<root available-locales=\"" + String.join(",", locales) + "\" default-locale=\"" + LocaleUtil.getDefault().toString() + "\">");
		content.append("<dynamic-element name=\"content\" type=\"text_area\" index-type=\"text\" instance-id=\"jpqu\">");
		for(String passedLocale : locales ) {
			content.append("<dynamic-content language-id=\"" + passedLocale + "\"><![CDATA[<p>");
			content.append(baseArticle);
			content.append("</p>]]></dynamic-content>");
		}
		content.append("</dynamic-element></root>");
		
		return content.toString();
	}
	
	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) {
		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
				WebKeys.THEME_DISPLAY);
		
		try {
			// Fetch data
			numberOfArticles = ParamUtil.getLong(actionRequest, "numberOfArticles", 1);
			baseTitle = ParamUtil.getString(actionRequest, "baseTitle", "");
			baseArticle = ParamUtil.getString(actionRequest, "baseArticle", "");
			folderId = ParamUtil.getLong(actionRequest, "folderId", 0);
			
			// Locales
			String[] defLang = { LocaleUtil.getDefault().toString() };
			locales = ParamUtil.getStringValues(actionRequest, "locales", defLang);
			
			// Sites
			groupId = ParamUtil.getLong(actionRequest, "groupId", themeDisplay.getScopeGroupId());
			
			// Create Web Contents
			createJournals(actionRequest, actionResponse);
		} catch (Throwable e) {
			hideDefaultSuccessMessage(actionRequest);
			e.printStackTrace();
		}

		actionResponse.setRenderParameter(
				"mvcRenderCommandName", LDFPortletKeys.COMMON);		
	}

	@Reference(unbind = "-")
	protected void setJournalArticleLocalService(JournalArticleLocalService journalArticleLocalService) {
		_journalArticleLocalService = journalArticleLocalService;
	}
	
	private JournalArticleLocalService _journalArticleLocalService;

	private long numberOfArticles = 0;
	private String baseTitle = "";
	private String baseArticle = "";
	private long groupId = 0;
	private long folderId = 0;
	private String[] locales;
}
