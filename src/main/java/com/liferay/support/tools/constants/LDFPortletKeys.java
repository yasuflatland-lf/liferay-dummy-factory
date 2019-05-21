package com.liferay.support.tools.constants;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LDFPortletKeys {
    public static final String EOL = System.getProperty("line.separator");

	// Portlet Name
	public static final String LIFERAY_DUMMY_FACTORY = "portlet_com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet";
	
	public static final String DUMMY_FACTORY_CONFIG = "com.liferay.support.tools.portlet.actions.DummyFactoryConfiguration";

	public static final String PORTLET_CONFIGURATION =
			"com_liferay_portlet_configuration_web_portlet_PortletConfigurationPortlet";
	
	@SuppressWarnings("serial")
	public static final Map<String,String> renderJSPs = Collections.unmodifiableMap(new ConcurrentHashMap<String, String>() {
		{put(LDFPortletKeys.MODE_ORGANIZAION, LDFPortletKeys.JSP_ORGANIZAION);}
		{put(LDFPortletKeys.MODE_SITES, LDFPortletKeys.JSP_SITES);}
		{put(LDFPortletKeys.MODE_PAGES, LDFPortletKeys.JSP_PAGES);}
		{put(LDFPortletKeys.MODE_USERS, LDFPortletKeys.JSP_USERS);}
		{put(LDFPortletKeys.MODE_WCM, LDFPortletKeys.JSP_WCM);}
		{put(LDFPortletKeys.MODE_DOCUMENTS, LDFPortletKeys.JSP_DOCUMENTS);}
		{put(LDFPortletKeys.MODE_MB, LDFPortletKeys.JSP_MB);}
		{put(LDFPortletKeys.MODE_CATEGORY, LDFPortletKeys.JSP_CATEGORY);}
		{put(LDFPortletKeys.MODE_BLOGS, LDFPortletKeys.JSP_BLOGS);}
		{put(LDFPortletKeys.MODE_WIKI, LDFPortletKeys.JSP_WIKI);}
		{put(LDFPortletKeys.MODE_COMPANY, LDFPortletKeys.JSP_COMPANY);}
    });
	
	// Mode parameter
	public static final String MODE = "mode";
	
	// Command
	public static final String COMMON = "/ldf/common";
	public static final String ORGANIZAION = "/ldf/org";
	public static final String SITES = "/ldf/sites";
	public static final String PAGES = "/ldf/pages";
	public static final String USERS = "/ldf/users";
	public static final String WCM = "/ldf/wcm";
	public static final String DOCUMENTS = "/ldf/doc";
	public static final String MB = "/ldf/mb";
	public static final String CATEGORY = "/ldf/category";
	public static final String BLOGS = "/ldf/blogs";
	public static final String WIKI = "/ldf/wiki";
	public static final String COMPANY = "/ldf/company";
	
	public static final String CMD_PAGES_FOR_A_SITE = "/ldf/page/for_a_site";
	public static final String CMD_ROLELIST = "/ldf/role/list";
	public static final String CMD_MB_LIST = "/ldf/mb/threads/list";
	public static final String CMD_CATEGORY_LIST = "/ldf/category/list";
	
	// Mode
	public static final String MODE_ORGANIZAION = "ORG";
	public static final String MODE_SITES = "SITES";
	public static final String MODE_PAGES = "PAGES";
	public static final String MODE_USERS = "USERS";
	public static final String MODE_WCM = "WCM";
	public static final String MODE_DOCUMENTS = "DOC";
	public static final String MODE_MB = "MB";
	public static final String MODE_CATEGORY = "CATEGORY";
	public static final String MODE_BLOGS = "BLOGS";
	public static final String MODE_WIKI = "WIKI";
	public static final String MODE_COMPANY = "COMPANY";
	
	// Jsps
    public static final String JSP_ORGANIZAION = "/view.jsp";
    public static final String JSP_SITES = "/sites.jsp";
    public static final String JSP_PAGES = "/pages.jsp";
    public static final String JSP_USERS = "/users.jsp";
    public static final String JSP_WCM = "/wcm.jsp";
    public static final String JSP_DOCUMENTS = "/documents.jsp";	
    public static final String JSP_MB = "/mb.jsp";	
    public static final String JSP_CATEGORY = "/category.jsp";	
    public static final String JSP_BLOGS = "/blogs.jsp";	
    public static final String JSP_WIKI = "/wiki.jsp";	
    public static final String JSP_COMPANY = "/company.jsp";	
    
    //WEB
    public static final String _DDM_STRUCTURE_KEY = "BASIC-WEB-CONTENT";
    public static final String _DDM_TEMPLATE_KEY = "BASIC-WEB-CONTENT";
    
    //Web contents contents mode
    public static final int WCM_SIMPLE_CONTENTS_CREATE = 0;
    public static final int WCM_DUMMY_CONTENTS_CREATE = 1;
    public static final int WCM_STRUCTURE_TEMPLATE_SELECT_CREATE = 2;

    //
    //Message board 
    //
    
    // contents mode
    public static final int MB_THREAD_CREATE = 0;
    public static final int MB_CATEGORY_CREATE = 1;
    public static final int MB_REPLY_CREATE = 2;
    
    // format
    public static final String MB_FORMAT_BBCODE = "bbcode";
    public static final String MB_FORMAT_HTML = "html";
    
    //Categroy / Vocabulary contents mode
    public static final int C_CATEGORY_CREATE = 0;
    public static final int C_VOCABULARY_CREATE = 1;
    
    // Wiki Node / Page mode
    public static final int W_NODE = 0;
    public static final int W_PAGE = 1;
    
}
