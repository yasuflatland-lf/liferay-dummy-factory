package com.liferay.support.tools.journal;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.support.tools.common.ParamContext;
import com.liferay.support.tools.utils.CommonUtil;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ActionRequest;

public class JournalContext extends ParamContext {
	private long numberOfArticles = 0;
	private String baseTitle = "";
	private String baseArticle = "";
	private long[] groupIds;
	private long folderId = 0;
	private String[] locales;
	private long createContentsType;
	private int totalParagraphs;
	private int titleWords;
	private int randomAmount;
	private String linkLists;
	private long ddmStructureId;
	private long ddmTemplateId;
	private Locale defaultLocale;
	private Map<Locale, String> descriptionMap;

	/**
	 * Constructor
	 * 
	 * @param actionRequest
	 * @throws PortalException
	 */
	public JournalContext(ActionRequest actionRequest) throws PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		// Fetch data
		numberOfArticles = ParamUtil.getLong(actionRequest, "numberOfArticles", 1);
		baseTitle = ParamUtil.getString(actionRequest, "baseTitle", "");
		baseArticle = ParamUtil.getString(actionRequest, "baseArticle", "");
		folderId = ParamUtil.getLong(actionRequest, "folderId", 0);
		totalParagraphs = ParamUtil.getInteger(actionRequest, "totalParagraphs", 0);
		titleWords = ParamUtil.getInteger(actionRequest, "titleWords", 0);
		randomAmount = ParamUtil.getInteger(actionRequest, "randomAmount", 0);
		createContentsType = ParamUtil.getLong(actionRequest, "createContentsType", 0);
		linkLists = ParamUtil.getString(actionRequest, "linkLists", "");
		ddmStructureId = ParamUtil.getLong(actionRequest, "ddmStructureId", 0);
		ddmTemplateId = ParamUtil.getLong(actionRequest, "ddmTemplateId", 0);

		// Locales
		String[] defLang = { LocaleUtil.getDefault().toString() };
		locales = ParamUtil.getStringValues(actionRequest, "locales", defLang);

		// Sites
		String[] groupsStrIds = ParamUtil.getStringValues(actionRequest, "groupIds",
				new String[] { String.valueOf(themeDisplay.getScopeGroupId()) });
		groupIds = CommonUtil.convertStringToLongArray(groupsStrIds);

		// Fetch default locale
		defaultLocale = LocaleUtil.fromLanguageId(themeDisplay.getUser().getLanguageId());

		descriptionMap = new ConcurrentHashMap<Locale, String>();
		descriptionMap.put(defaultLocale, StringPool.BLANK);
	}

	public long getNumberOfArticles() {
		return numberOfArticles;
	}

	public void setNumberOfArticles(long numberOfArticles) {
		this.numberOfArticles = numberOfArticles;
	}

	public String getBaseTitle() {
		return baseTitle;
	}

	public void setBaseTitle(String baseTitle) {
		this.baseTitle = baseTitle;
	}

	public String getBaseArticle() {
		return baseArticle;
	}

	public void setBaseArticle(String baseArticle) {
		this.baseArticle = baseArticle;
	}

	public long[] getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(long[] groupIds) {
		this.groupIds = groupIds;
	}

	public long getFolderId() {
		return folderId;
	}

	public void setFolderId(long folderId) {
		this.folderId = folderId;
	}

	public String[] getLocales() {
		return locales;
	}

	public void setLocales(String[] locales) {
		this.locales = locales;
	}

	public long getCreateContentsType() {
		return createContentsType;
	}

	public void setCreateContentsType(long createContentsType) {
		this.createContentsType = createContentsType;
	}

	public int getTotalParagraphs() {
		return totalParagraphs;
	}

	public void setTotalParagraphs(int totalParagraphs) {
		this.totalParagraphs = totalParagraphs;
	}

	public int getTitleWords() {
		return titleWords;
	}

	public void setTitleWords(int titleWords) {
		this.titleWords = titleWords;
	}

	public int getRandomAmount() {
		return randomAmount;
	}

	public void setRandomAmount(int randomAmount) {
		this.randomAmount = randomAmount;
	}

	public String getLinkLists() {
		return linkLists;
	}

	public void setLinkLists(String linkLists) {
		this.linkLists = linkLists;
	}

	public long getDdmStructureId() {
		return ddmStructureId;
	}

	public void setDdmStructureId(long ddmStructureId) {
		this.ddmStructureId = ddmStructureId;
	}

	public long getDdmTemplateId() {
		return ddmTemplateId;
	}

	public void setDdmTemplateId(long ddmTemplateId) {
		this.ddmTemplateId = ddmTemplateId;
	}

	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public Map<Locale, String> getDescriptionMap() {
		return descriptionMap;
	}

	public void setDescriptionMap(Map<Locale, String> descriptionMap) {
		this.descriptionMap = descriptionMap;
	}
}
