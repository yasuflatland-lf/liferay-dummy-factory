<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/frontend" prefix="liferay-frontend" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/security" prefix="liferay-security" %>
<%@ taglib uri="http://liferay.com/tld/item-selector" prefix="liferay-item-selector" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/trash" prefix="liferay-trash" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>
<%@ taglib uri="http://liferay.com/tld/clay" prefix="clay" %>

<%@ page import="com.liferay.portal.kernel.util.*" %>
<%@ page import="com.liferay.portal.kernel.model.*" %>
<%@ page import="com.liferay.portal.kernel.portlet.*" %>
<%@ page import="com.liferay.portal.kernel.service.*" %>
<%@ page import="com.liferay.portal.kernel.search.*" %>
<%@ page import="com.liferay.portal.kernel.module.configuration.ConfigurationProviderUtil" %>
<%@ page import="com.liferay.portal.kernel.dao.orm.QueryUtil" %>
<%@ page import="com.liferay.portal.kernel.dao.search.*" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.workflow.*" %>
<%@ page import="com.liferay.document.library.kernel.model.*" %>
<%@ page import="com.liferay.document.library.kernel.service.*" %>
<%@ page import="com.liferay.document.library.configuration.*" %>

<%@ page import="com.liferay.taglib.search.ResultRow" %>
<%@ page import="com.liferay.taglib.search.SearchEntry" %>

<%@ page import="com.liferay.support.tools.portlet.actions.PageMVCResourceCommand" %>
<%@ page import="com.liferay.support.tools.portlet.actions.RoleMVCResourceCommand" %>
<%@ page import="com.liferay.support.tools.messageboard.MBMVCResourceCommand" %>
<%@ page import="com.liferay.support.tools.utils.WikiCommons" %>

<%@ page import="com.liferay.support.tools.constants.*" %>
<%@ page import="com.liferay.asset.kernel.model.*" %>
<%@ page import="com.liferay.asset.kernel.service.*" %>

<%@ page import="com.liferay.support.tools.portlet.actions.DummyFactoryConfiguration" %>
<%@ page import="com.liferay.dynamic.data.mapping.model.*" %>
<%@ page import="com.liferay.journal.model.*" %>
<%@ page import="com.liferay.journal.service.*" %>
<%@ page import="com.liferay.dynamic.data.mapping.service.*" %>
<%@ page import="com.liferay.message.boards.model.*" %>
<%@ page import="com.liferay.message.boards.constants.*" %>
<%@ page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil" %>
<%@ page import="com.liferay.support.tools.document.library.EditFileEntryMVCActionCommand" %>
<%@ page import="com.liferay.portal.util.PropsValues" %>
<%@ page import="com.liferay.announcements.kernel.model.*" %>
<%@ page import="com.liferay.portlet.announcements.model.impl.*" %>
<%@ page import="com.liferay.support.tools.display.context.*" %>
<%@ page import="com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver" %>
<%@ page import="com.liferay.frontend.js.loader.modules.extender.npm.JSPackage" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.text.DecimalFormatSymbols" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.petra.string.StringPool" %>
<%@ page import="com.liferay.portal.kernel.exception.*" %>
<%@ page import="com.liferay.asset.kernel.exception.*" %>

<liferay-frontend:defineObjects/>
<liferay-theme:defineObjects/>
<portlet:defineObjects/>

<%
	PortletURL portletURL = PortletURLUtil.clone(renderResponse.createRenderURL(), liferayPortletResponse);
	String redirect = ParamUtil.getString(request, "redirect");
	//Mode
	String mode = ParamUtil.getString(request, LDFPortletKeys.MODE, LDFPortletKeys.MODE_ORGANIZAION);
	
	// Generate Progress ID
	String progressId = PortalUtil.generateRandomKey(request, "progressId");
	
	DummyFactoryConfiguration dummyFactoryConfiguration =
	        (DummyFactoryConfiguration)
	            renderRequest.getAttribute(DummyFactoryConfiguration.class.getName());


    String linkList = StringPool.BLANK;
    String urlList = StringPool.BLANK;

    if (Validator.isNotNull(dummyFactoryConfiguration)) {

        linkList =
            HtmlUtil.escape(
                portletPreferences.getValue(
                    "linkList", dummyFactoryConfiguration.linkList()));
        
        urlList =
                HtmlUtil.escape(
                    portletPreferences.getValue(
                        "urlList", dummyFactoryConfiguration.urlList()));        
    }

	DummyFactoryDisplayContext dummyFactoryDisplayContext = new DummyFactoryDisplayContext(request, liferayPortletRequest, liferayPortletResponse, portletPreferences);
	
	String lodashResolver =
			(String)renderRequest.getAttribute("lodashResolver");
	String jqueryResolver =
			(String)renderRequest.getAttribute("jqueryResolver");
%>
<aui:script use="aui-base" sandbox="<%= true %>">
    Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
        (function() {
			var _ = _lodash;
			
			//Convert bracket for Lodash template to avoid overraping jsp tag.
			_.templateSettings = {
			    interpolate: /\<\@\=(.+?)\@\>/gim,
			    evaluate: /\<\@([\s\S]+?)\@\>/gim,
			    escape: /\<\@\-(.+?)\@\>/gim
			};
        })()
    }, function(error) {
        console.error(error)
    });	
</aui:script>
