<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@ include file="/init.jsp" %>
<%@ page contentType="text/html; charset=UTF-8" %>

<liferay-portlet:actionURL portletConfiguration="<%= true %>"
                           var="configurationActionURL"
/>

<liferay-portlet:renderURL portletConfiguration="<%= true %>"
                           var="configurationRenderURL" />
<aui:form action="<%= configurationActionURL %>" method="post" name="fm">
    <div class="portlet-configuration-body-content">
        <div class="container-fluid-1280">
			<p>No Configrations for now. If you have a usecase to use a configration, please cleate a issue <a href="https://github.com/yasuflatland-lf/liferay-dummy-factory/issues">here</a></p>		
        </div>
    </div>
</aui:form>
