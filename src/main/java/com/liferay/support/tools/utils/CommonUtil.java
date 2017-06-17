package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

@Component(service = CommonUtil.class)
public class CommonUtil {
	
	@SuppressWarnings("serial")
	private static final Map<String,String> renderJSPs = new HashMap<String, String>() {
		{put(LDFPortletKeys.MODE_ORGANIZAION, LDFPortletKeys.JSP_ORGANIZAION);}
		{put(LDFPortletKeys.MODE_SITES, LDFPortletKeys.JSP_SITES);}
		{put(LDFPortletKeys.MODE_PAGES, LDFPortletKeys.JSP_PAGES);}
		{put(LDFPortletKeys.MODE_USERS, LDFPortletKeys.JSP_USERS);}
		{put(LDFPortletKeys.MODE_WCM, LDFPortletKeys.JSP_WCM);}
		{put(LDFPortletKeys.MODE_DOCUMENTS, LDFPortletKeys.JSP_DOCUMENTS);}
    };

	/**
	 * Page Command Pairs
	 * 
	 * @return jsp file name corresponding to the command.
	 */
	public Map<String,String> getPageFromMode() {
		return renderJSPs;
	}

	/**
	 * Convert string array to long array
	 * 
	 * @param source String array of ids
	 * @return long array of ids
	 */
	public long[] convertStringToLongArray(String[] source) {
		if(null == source || source.length <= 0) {
			return null;
		}
		
		return Arrays.stream(source).mapToLong(Long::parseLong).toArray();
	}	

}
