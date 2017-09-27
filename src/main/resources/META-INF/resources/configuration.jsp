
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
            <aui:fieldset-group markupView="lexicon">
                <aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>" />
                <aui:input name="redirect" type="hidden" value="<%= configurationRenderURL %>" />

				<%
				String urlListLabel = "URL list where the crawler fetching images from (multiple URLs can be configured by comma separated)";
				String linkListLabel = "Corrected image links / custom image links";

				%>
				<div class="row">
					<aui:fieldset cssClass="col-md-4">
	                    <aui:input type="textarea" name="urlList" value="<%= urlList %>" rows="10" label="<%=urlListLabel %>" />
						<button id="<portlet:namespace />fetchLinks" class="btn btn-primary loading" type="button">
			                 <span id="<portlet:namespace />linkLoader" class="loading-icon linear hide"></span>&nbsp;Fetch links
			            </button>																								
					</aui:fieldset>
					<aui:fieldset cssClass="col-md-8">
	                    <aui:input type="textarea" name="linkList" value="<%= linkList %>" rows="10" label="<%=linkListLabel %>" />
					</aui:fieldset>
				</div>				
            </aui:fieldset-group>
        </div>
    </div>
    <aui:button-row>
        <aui:button type="submit"></aui:button>
    </aui:button-row>
</aui:form>

<portlet:resourceURL id="/ldf/image/list" var="linkListURL" />

<aui:script use="aui-base">
	var linkLoader = A.one('#<portlet:namespace />linkLoader');
    var fetchLinks = A.one('#<portlet:namespace />fetchLinks');
    var linkList = A.one('#<portlet:namespace />linkList');
    var urlList = A.one('#<portlet:namespace />urlList');
    
    fetchLinks.on(
        'click',
        function() {
            event.preventDefault();
            Liferay.Util.toggleDisabled('#<portlet:namespace />fetchLinks', true);
            linkLoader.show();
            var data = Liferay.Util.ns(
                '<portlet:namespace />',
                {
                    numberOfCrawlers: 10,
                    maxDepthOfCrawling: 2,
                    maxPagesToFetch: 100,
                    urls: urlList.val()
                }
            );
            
			$.ajax(
                '<%= linkListURL.toString() %>',
                {
                    data: data,
                    success: function(data) {
                    	var currentText = linkList.val();
                    	linkList.val(currentText + data.urlstr);
			            Liferay.Util.toggleDisabled('#<portlet:namespace />fetchLinks', false);
			            linkLoader.hide();
                    }
                }
            );
          
        }
    );  
	
</aui:script>