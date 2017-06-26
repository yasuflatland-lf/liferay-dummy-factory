package com.liferay.support.tools.constants;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LDFPortletKeys {
	// Portlet Name
	public static final String LIFERAY_DUMMY_FACTORY = "portlet_com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet";
	
	@SuppressWarnings("serial")
	public static final Map<String,String> renderJSPs = Collections.unmodifiableMap(new ConcurrentHashMap<String, String>() {
		{put(LDFPortletKeys.MODE_ORGANIZAION, LDFPortletKeys.JSP_ORGANIZAION);}
		{put(LDFPortletKeys.MODE_SITES, LDFPortletKeys.JSP_SITES);}
		{put(LDFPortletKeys.MODE_PAGES, LDFPortletKeys.JSP_PAGES);}
		{put(LDFPortletKeys.MODE_USERS, LDFPortletKeys.JSP_USERS);}
		{put(LDFPortletKeys.MODE_WCM, LDFPortletKeys.JSP_WCM);}
		{put(LDFPortletKeys.MODE_DOCUMENTS, LDFPortletKeys.JSP_DOCUMENTS);}
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
	public static final String CMD_PAGES_FOR_A_SITE = "/ldf/page/for_a_site";
	public static final String CMD_ROLELIST = "/ldf/role/list";
	
	// Mode
	public static final String MODE_ORGANIZAION = "ORG";
	public static final String MODE_SITES = "SITES";
	public static final String MODE_PAGES = "PAGES";
	public static final String MODE_USERS = "USERS";
	public static final String MODE_WCM = "WCM";
	public static final String MODE_DOCUMENTS = "DOC";
	
	// Jsps
    public static final String JSP_ORGANIZAION = "/view.jsp";
    public static final String JSP_SITES = "/sites.jsp";
    public static final String JSP_PAGES = "/pages.jsp";
    public static final String JSP_USERS = "/users.jsp";
    public static final String JSP_WCM = "/wcm.jsp";
    public static final String JSP_DOCUMENTS = "/documents.jsp";	
    
    //WEB
    public static final String _DDM_STRUCTURE_KEY = "BASIC-WEB-CONTENT";
    public static final String _DDM_TEMPLATE_KEY = "BASIC-WEB-CONTENT";
    
}
