<%-- NOTE: DXP 2026 only advertises Provide-Capability for http://xmlns.jcp.org/portlet_3_0.
     Switching to jakarta.tags.portlet breaks bundle resolution. Do not change the URI.
     See docs/ADR/adr-0008-dxp-2026-migration.md. --%>
<%@ taglib uri="http://xmlns.jcp.org/portlet_3_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/react" prefix="react" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>

<%@ page import="com.liferay.portal.kernel.json.JSONFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.json.JSONObject" %>
<%@ page import="com.liferay.portal.kernel.util.HashMapBuilder" %>

<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.ResourceBundle" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />
