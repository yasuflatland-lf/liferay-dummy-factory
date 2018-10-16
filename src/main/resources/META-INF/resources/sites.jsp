<%@ include file="/init.jsp"%>

<aui:nav-bar cssClass="collapse-basic-search" markupView="lexicon">
	<aui:nav cssClass="navbar-nav">
		<aui:nav-item label="Sites" selected="<%= true %>" />
	</aui:nav>
</aui:nav-bar>

<div class="container-fluid-1280">
	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
		
			<liferay-ui:success key="success" message="Sites created successfully" />
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.SITES %>" var="siteEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_SITES %>" />		
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>
			
			<div class="entry-title form-group">
				<h1>Create Sites&nbsp;&nbsp;
					<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
					</a>
				</h1>
			</div>
		
			<div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
				<blockquote class="blockquote-info">
				    <small>Example</small>
					<p>if you enter the values <code>3</code> and "site" the portlet will create three blank sites: <code>site1</code>, <code>site2</code>, and <code>site3</code>.<p>
				</blockquote>
			
				<ul>
					<li>You must be signed in as an administrator in order to create sites</li>
					<li>The counter always starts at <code>1</code></li>
					<li>The site type is <code>Blank Site</code></li>
				</ul>
			
				<h3>Creating Large Batches of Sites</h3>
				<ul>
					<li>If the number of sites is large (over <code>100</code>), go to <i>Control Panel -> Server Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code> and set to <code>INFO</code> to track progress (batches of 10%)</li>
					<li>It may take some time (even for the logs to show) to create a large number of sites, and the page will hang until the process is complete; you can query the database if you are uncertain of the progress</li>
				</ul>
			</div>

			<%
			String numberOfSitesLabel = "Enter the number of sites you would like to create";
			String baseSiteNameLabel = "Enter the base name for the sites";
			String siteTypeLabel = "Site type";
			%>

			<aui:form action="<%= siteEditURL %>" method="post" name="fm">
			    <aui:select name="siteType" label="<%=siteTypeLabel %>" > 
			        <aui:option label="<%=GroupConstants.TYPE_SITE_OPEN_LABEL %>" value="<%= GroupConstants.TYPE_SITE_OPEN %>" />
			        <aui:option label="<%=GroupConstants.TYPE_SITE_PRIVATE_LABEL %>" value="<%= GroupConstants.TYPE_SITE_PRIVATE %>" />
			        <aui:option label="<%=GroupConstants.TYPE_SITE_RESTRICTED_LABEL %>" value="<%= GroupConstants.TYPE_SITE_RESTRICTED %>" />
			    </aui:select>			
				<aui:input name="numberOfSites" label="<%= numberOfSitesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseSiteName" label="<%= baseSiteNameLabel %>" >
					<aui:validator name="required" />				
				</aui:input>
		
				<%
				String parentGroupIdLabel = "Select the parent site";
				String manualMembershipLabel = "Enable manual membership";
				String inheritContentLabel = "Enable inherit content";
				String activeLabel = "Activate site";
				String publicLayoutSetPrototypeIdLabel = "Site Template";
				
				List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
				String scopeGroupdId = String.valueOf(themeDisplay.getScopeGroupId());
				String defaultOption = "(None)";
				
				List<LayoutSetPrototype> layoutSetPrototypes = LayoutSetPrototypeLocalServiceUtil.search(
					themeDisplay.getCompanyId(), true,
					QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);				
				%>
		
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-6">
							<aui:select name="parentGroupId" label="<%= parentGroupIdLabel %>" >
								<aui:option label="<%= defaultOption %>" value="<%= GroupConstants.DEFAULT_PARENT_GROUP_ID %>" />
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
							
							<aui:select name="publicLayoutSetPrototypeId" label="<%= publicLayoutSetPrototypeIdLabel %>" >
								<aui:option label="<%= defaultOption %>" value="<%= 0 %>" />
								<%
								for (LayoutSetPrototype layoutSetPrototype : layoutSetPrototypes) {
								%>
									<aui:option label="<%= layoutSetPrototype.getName(locale) %>" value="<%= layoutSetPrototype.getLayoutSetPrototypeId() %>"/>
								<%
								}
								%>
							</aui:select>													
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input type="toggle-switch" name="manualMembership" label="<%= manualMembershipLabel %>" value="true" />
							<aui:input type="toggle-switch" name="inheritContent" label="<%= inheritContentLabel %>" value="false" />
							<aui:input type="toggle-switch" name="active" label="<%= activeLabel %>" value="true" />
						</aui:fieldset>
					</div>
				</div>	
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