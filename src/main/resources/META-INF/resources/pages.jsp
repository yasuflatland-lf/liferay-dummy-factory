<%@ include file="/init.jsp"%>

<clay:navigation-bar
	inverted="<%= true %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Pages") %>'
/>

<div class="container-fluid-1280">

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
		
			<liferay-ui:success key="success" message="Pages created successfully" />
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.PAGES %>" var="pageEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_PAGES %>" />	
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>		
			<div class="entry-title form-group">
				<h1>Create Pages&nbsp;&nbsp;
					<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
					</a>
				</h1>
			</div>
		
			<div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
				<blockquote class="blockquote-info">
				    <small>Example</small>
					<p>if you enter the values <code>3</code> and "page" the portlet will create three pages: <code>page1</code>, <code>page2</code>, and <code>page3</code>.<p>
				</blockquote>
			
				<ul>
					<li>You must be signed in as an administrator in order to create pages</li>
					<li>The counter always starts at <code>1</code></li>
					<li>If no site is selected, the default site will be <code>liferay.com</code></li>
				</ul>
			
			</div>

			<%
			String numberOfPagesLabel= "Enter the number of pages you would like to create";
			String basePageNameLabel= "Enter the base page name (i.e. newPage, page, testPage)";
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			String defaultOption = "(None)";
			
			String scopeGroupdId = String.valueOf(themeDisplay.getScopeGroupId());
			String groupLabel = "Select a site to assign the pages to";
			String pageLabel = "Select a parent page";
			%>

			<aui:form action="<%= pageEditURL %>" method="post" name="fm">
				<aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>
			
				<aui:input name="numberOfpages" label="<%= numberOfPagesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="basePageName" label="<%= basePageNameLabel %>" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="group" label="<%= groupLabel %>" >
					<aui:option label="<%= defaultOption %>" value="<%= scopeGroupdId %>" />
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


				<%
				String parentLayoutIdLabel = "Enter the parent page ID";
				String privateLayoutLabel = "Make pages private";
				String hiddenLabel = "Hide from Navigation Menu";
				%>
		
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-6">

							<aui:select name="parentLayoutId" label="<%=pageLabel %>" >
								<aui:option label="<%= defaultOption %>" value="<%= LayoutConstants.DEFAULT_PARENT_LAYOUT_ID %>" />
							</aui:select>			
							<aui:input type="hidden" name="layoutType" label="<%= privateLayoutLabel %>" value="<%= LayoutConstants.TYPE_PORTLET %>" />
												
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input type="toggle-switch" name="privateLayout" label="<%= privateLayoutLabel %>" value="false" />
							<aui:input type="toggle-switch" name="hidden" label="<%= hiddenLabel %>" value="false" />
						</aui:fieldset>
					</div>
				</div>	
				<aui:button-row>
					<aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary" id="processStart"/>
				</aui:button-row>	
			</aui:form>	
<%
// Because of bug of lifeary-ui:upload-progress, you need to add the following parameter in the request.
String progressSessionKey = ProgressTracker.PERCENT + progressId;
request.setAttribute("liferay-ui:progress:sessionKey", progressSessionKey);
%>			
			<liferay-ui:upload-progress
				id="<%= progressId %>"
				message="creating..."
				height="20"
			/>			
		</aui:fieldset>
	</aui:fieldset-group>
</div>

<aui:script use="aui-base">
	var processStart = A.one('#<portlet:namespace />processStart');
	
	processStart.on(
	    'click',
	    function() {
	    	event.preventDefault();
			<%= progressId %>.startProgress();
			submitForm(document.<portlet:namespace />fm);
	    }
	);
</aui:script>

<portlet:resourceURL id="<%=LDFPortletKeys.CMD_PAGES_FOR_A_SITE %>" var="pagesForASiteURL" />

<script type="text/html" id="<portlet:namespace />page_per_site_options">
    <option value="<@= parentLayoutId @>"><@= name @></option>
</script>

<aui:script use="aui-base">
	$('#<portlet:namespace />group').on(
		'change',
		function(event) {
			var data = Liferay.Util.ns(
				'<portlet:namespace />',
				{
					<%=Constants.CMD %>: '<%=PageMVCResourceCommand.CMD_PAGELIST%>',
					siteGroupId: $('#<portlet:namespace />group').val()
				}
			);

			$.ajax(
				'<%= pagesForASiteURL.toString() %>',
				{
					data: data,
					success: function(data) {
						
						//Load Template
						var tmpl = _.template($('#<portlet:namespace />page_per_site_options').html());
						var listAll = 
						tmpl({
							name:'(None)',
							parentLayoutId:0
						});
						_.map(data,function(n) {
							listAll += 
							tmpl(
							  {
								name:(n.name) ? _.escape(n.name) : "",
								parentLayoutId:(n.parentLayoutId) ? _.escape(n.parentLayoutId) : ""
							  }
							);
						});
						var pageObj = $('#<portlet:namespace />parentLayoutId');
						pageObj.empty();
						pageObj.append(listAll);
					}
				}
			);
		}
	);
</aui:script>
