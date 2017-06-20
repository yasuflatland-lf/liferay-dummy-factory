<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
	<liferay-ui:success key="success" message="Pages created successfully" />
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.PAGES %>" var="pageEditURL">
		<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_PAGES %>" />	
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
			<div class="entry-title form-group">
				<h1>Create Pages&nbsp;&nbsp;
					<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
					</a>
				</h1>
			</div>
		
			<div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
				<blockquote class="blockquote-info">
				    <small>Example</small>
					<p>if you enter the values <code>3</code> and "page" the portlet will create three blank sites: <code>page1</code>, <code>page2</code>, and <code>page3</code>.<p>
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
			
			%>

			<aui:form action="<%= pageEditURL %>" method="post" >
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

							<aui:input name="parentLayoutId" label="<%= parentLayoutIdLabel %>" value="<%=LayoutConstants.DEFAULT_PARENT_LAYOUT_ID %>">
								<aui:validator name="digits" />
							</aui:input>				
							<aui:input type="hidden" name="layoutType" label="<%= privateLayoutLabel %>" value="<%= LayoutConstants.TYPE_PORTLET %>" />
							<%-- 
							// This function is not really necessarily, so fix value to portlet for now.
							<aui:select name="layoutType" label="<%= groupLabel %>">
								<aui:option label="Portlet" value="<%= LayoutConstants.TYPE_PORTLET %>"/>
								<aui:option label="Control Panel" value="<%= LayoutConstants.TYPE_CONTROL_PANEL %>"/>
								<aui:option label="Embedded" value="<%= LayoutConstants.TYPE_EMBEDDED %>"/>
								<aui:option label="Link to Layout" value="<%= LayoutConstants.TYPE_LINK_TO_LAYOUT %>"/>
								<aui:option label="Panel" value="<%= LayoutConstants.TYPE_PANEL %>"/>
								<aui:option label="Shared Portlet" value="<%= LayoutConstants.TYPE_SHARED_PORTLET %>"/>
								<aui:option label="URL" value="<%= LayoutConstants.TYPE_URL %>"/>
							</aui:select>
							--%>													
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input type="toggle-switch" name="privateLayout" label="<%= privateLayoutLabel %>" value="false" />
							<aui:input type="toggle-switch" name="hidden" label="<%= hiddenLabel %>" value="false" />
						</aui:fieldset>
					</div>
				</div>	
				<aui:button-row>
					<aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary"/>
				</aui:button-row>	
			</aui:form>	
		</aui:fieldset>
	</aui:fieldset-group>
</div>
