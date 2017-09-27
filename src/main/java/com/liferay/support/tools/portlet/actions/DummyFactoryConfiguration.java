package com.liferay.support.tools.portlet.actions;

import com.liferay.support.tools.constants.LDFPortletKeys;

import aQute.bnd.annotation.metatype.Meta;

/**
 * Dummy Factory Configuration
 *
 * @author Yasuyuki Takeo
 */
@Meta.OCD(id = LDFPortletKeys.DUMMY_FACTORY_CONFIG)
public interface DummyFactoryConfiguration {
    public static final String CONF_LINKLIST = "linkList";
    public static final String CONF_URLLIST = "urlList";
    
    @Meta.AD(deflt = "", required = false)
    public String linkList();

    @Meta.AD(deflt = "https://www.shutterstock.com/photos", required = false)
    public String urlList();
    
}
