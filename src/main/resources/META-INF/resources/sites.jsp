<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
	<liferay-ui:success key="success" message="Sites created successfully" />
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.SITES %>" var="siteEditURL">
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
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

			<aui:form action="<%= siteEditURL %>" method="post" >
			    <aui:select name="siteType" label="<%=siteTypeLabel %>" > 
			        <aui:option label="<%=GroupConstants.TYPE_SITE_OPEN_LABEL %>" value="<%= GroupConstants.TYPE_SITE_OPEN %>" />
			        <aui:option label="<%=GroupConstants.TYPE_SITE_PRIVATE_LABEL %>" value="<%= GroupConstants.TYPE_SITE_PRIVATE %>" />
			        <aui:option label="<%=GroupConstants.TYPE_SITE_RESTRICTED_LABEL %>" value="<%= GroupConstants.TYPE_SITE_RESTRICTED %>" />
			        <aui:option label="<%=GroupConstants.TYPE_SITE_SYSTEM_LABEL %>" value="<%= GroupConstants.TYPE_SITE_SYSTEM %>" />
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
				String parentGroupIdLabel = "Enter the parent group ID";
				String liveGroupIdLabel = "Enter the live group ID";
				String manualMembershipLabel = "Enable manual membership";
				String siteLabel = "Enable site";
				String inheritContentLabel = "Enable inherit content";
				String activeLabel = "Activate site";
				%>
		
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-6">
							<aui:input name="parentGroupId" label="<%= parentGroupIdLabel %>" value="<%=GroupConstants.DEFAULT_PARENT_GROUP_ID %>">
								<aui:validator name="digits" />
							</aui:input>				
							<aui:input name="liveGroupId" label="<%= liveGroupIdLabel %>" value="<%=GroupConstants.DEFAULT_LIVE_GROUP_ID %>">
								<aui:validator name="digits" />
							</aui:input>				
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input type="checkbox" name="manualMembership" label="<%= manualMembershipLabel %>" value="false" />
							<aui:input type="checkbox" name="site" label="<%= siteLabel %>" value="true" />
							<aui:input type="checkbox" name="inheritContent" label="<%= inheritContentLabel %>" value="false" />
							<aui:input type="checkbox" name="active" label="<%= activeLabel %>" value="true" />
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
