<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.PAGES %>" var="pageEditURL">
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
			
				<h3>Creating Large Batches of Pages</h3>
				<ul>
					<li>If the number of pages is large (over <code>100</code>), go to <i>Control Panel -> Server Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code> and set to <code>INFO</code> to track progress (batches of 10%)</li>
					<li>It may take some time (even for the logs to show) to create a large number of pages, and the page will hang until the process is complete; you can query the database if you are uncertain of the progress</li>
				</ul>
			</div>

			<%
			String numberOfPagesLabel= "Enter the number of pages you would like to create";
			String basePageNameLabel= "Enter the base page name (i.e. newPage, page, testPage)";
			%>

			<aui:form action="<%= pageEditURL %>" method="post" >
				<aui:input name="numberOfPages" label="<%= numberOfPagesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="basePageName" label="<%= basePageNameLabel %>" >
					<aui:validator name="required" />				
				</aui:input>
		
				<%
				List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
				String scopeGroupdId = String.valueOf(themeDisplay.getScopeGroupId());
				String groupLabel = "Select a site to assign the pages to";
				String defaultOption = "(None)";
				String parentLayoutIdLabel = "Enter the primary key of the parent layout";
				String privateLayoutLabel = "Enable privateLayout whether the layout is private to the group";
				String hiddenLabel = "Enable to make this layout is hidden";
				%>
		
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-6">
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
							<aui:input name="parentLayoutId" label="<%= parentLayoutIdLabel %>" value="<%=LayoutConstants.DEFAULT_PARENT_LAYOUT_ID %>">
								<aui:validator name="digits" />
							</aui:input>				
							<aui:select name="layoutType" label="<%= groupLabel %>" >
								<aui:option label="Portlet" value="<%= LayoutConstants.TYPE_PORTLET %>"/>
								<aui:option label="Control Panel" value="<%= LayoutConstants.TYPE_CONTROL_PANEL %>"/>
								<aui:option label="Embedded" value="<%= LayoutConstants.TYPE_EMBEDDED %>"/>
								<aui:option label="Link to Layout" value="<%= LayoutConstants.TYPE_LINK_TO_LAYOUT %>"/>
								<aui:option label="Panel" value="<%= LayoutConstants.TYPE_PANEL %>"/>
								<aui:option label="Shared Portlet" value="<%= LayoutConstants.TYPE_SHARED_PORTLET %>"/>
								<aui:option label="URL" value="<%= LayoutConstants.TYPE_URL %>"/>
							</aui:select>													
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input type="checkbox" name="privateLayout" label="<%= privateLayoutLabel %>" value="false" />
							<aui:input type="checkbox" name="hidden" label="<%= hiddenLabel %>" value="false" />
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
