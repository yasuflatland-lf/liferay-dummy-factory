<%@ include file="/init.jsp"%>

<aui:nav-bar cssClass="collapse-basic-search" markupView="lexicon">
	<aui:nav cssClass="navbar-nav">
		<aui:nav-item label="Wiki" selected="<%= true %>" />
	</aui:nav>
</aui:nav-bar>
	
<div class="container-fluid-1280">

	<liferay-ui:success key="success" message="Wiki page / node created successfully" />
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.WIKI %>" var="wikiEditURL">
		<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_WIKI %>" />
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>	

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>

			<%
			String numberOfnodesLabel = "Enter the number of wiki nodes you would like to create";
			String baseNodeNameLabel = "Enter the base name for the node";
			String groupIdLabel = "Chose site";
			String createContentsTypeLabel = "Select create type";
			
			String numberOfpagesLabel = "Enter the number of wiki pages you would like to create";
			String basePageNameLabel = "Enter the base name for the page";
			String baseContentNameLabel = "Enter the base ontent for the page";
			String baseSummaryNameLabel = "Enter the base summary for the page";
			String minorEditLabel = "Create this page as minor edit";			

					// Set Guest group ID for scope group ID
			final long guestGroupId = GroupLocalServiceUtil
					.getGroup(themeDisplay.getCompanyId(), GroupConstants.GUEST).getGroupId();
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			String defaultOption = "(None)";
			
			%>

			<aui:form action="<%= wikiEditURL %>" method="post" name="fm" >
				<aui:select name="createContentsType" label="<%= createContentsTypeLabel %>" >
					<aui:option selected="true" label="Wiki Node" value="<%= String.valueOf(LDFPortletKeys.W_NODE) %>" />
					<aui:option label="Wiki Page" value="<%= String.valueOf(LDFPortletKeys.W_PAGE) %>" />
				</aui:select>
							
				<aui:select name="groupId" label="<%= groupIdLabel %>" >
					<aui:option label="<%= defaultOption %>" value="<%= guestGroupId %>" />
					<%
					for (Group group : groups) {
						if (group.isSite()) {
					%>
							<aui:option label="<%= group.getDescriptiveName() %>" value="<%= group.getGroupId() %>"/>
					<%
						}
					}
					%>
				</aui:select>	
											
				<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.W_NODE) %>" class="<portlet:namespace />contentsTypeGroup" >
					<aui:input name="numberOfnodes" label="<%= numberOfnodesLabel %>" >
						<aui:validator name="digits" />
						<aui:validator name="min">1</aui:validator>
						<aui:validator name="required" />				
					</aui:input>
					
					<aui:input name="baseNodeName" label="<%= baseNodeNameLabel %>" >
						<aui:validator name="required" />				
					</aui:input>
			
				</span>
				<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.W_PAGE) %>" class="<portlet:namespace />contentsTypeGroup" style="display:none;">
					<aui:input name="numberOfpages" label="<%= numberOfpagesLabel %>" >
						<aui:validator name="digits" />
						<aui:validator name="min">1</aui:validator>
				        <aui:validator name="required">
			                function() {
		                        return (<%= String.valueOf(LDFPortletKeys.W_PAGE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
			                }
				        </aui:validator>				
					</aui:input>
					
					<aui:input name="basePageName" label="<%= basePageNameLabel %>" >
				        <aui:validator name="required">
			                function() {
		                        return (<%= String.valueOf(LDFPortletKeys.W_PAGE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
			                }
				        </aui:validator>				
					</aui:input>		
					
					<aui:input name="baseContentName" label="<%= baseContentNameLabel %>" />								
					<aui:input name="baseSummaryName" label="<%= baseSummaryNameLabel %>" />	
					<aui:input name="minorEdit" type="toggle-switch" label="<%= minorEditLabel %>" value="<%= false %>"/>							
				</span>

				<aui:button-row>
					<aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary" id="processStart"/>
				</aui:button-row>	
			</aui:form>	
			
			<liferay-ui:upload-progress
				id="<%= progressId %>"
				message="creating..."
			/>	
				
		</aui:fieldset>	
	</aui:fieldset-group>
		
</div>

<aui:script use="aui-base, liferay-form">
	// Generate dummy data
	$('#<portlet:namespace />processStart').on(
	    'click',
	    function() {
	    	event.preventDefault();
			<%= progressId %>.startProgress();
			submitForm(document.<portlet:namespace />fm);
	    }
	)
	
    // Manage GroupID list display
    var createContentsType = A.one('#<portlet:namespace />createContentsType');
	$('#<portlet:namespace />createContentsType').on(
	    'change load',
	    function() {
	    	//--------------------------------
	    	// Contents Creation fields switch
	    	//--------------------------------
    		var cmp_str = "<portlet:namespace />contentsType" + createContentsType.val();
	    	$('.<portlet:namespace />contentsTypeGroup').each(function(index){
				$(this).toggle((cmp_str === $(this).attr("id")));
	    	});
	    }
	);   	
</aui:script>
