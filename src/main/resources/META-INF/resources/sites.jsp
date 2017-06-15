<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
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
					<li>It may take some time (even for the logs to show) to create a large number of organizations, and the page will hang until the process is complete; you can query the database if you are uncertain of the progress</li>
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
					<aui:validator errorMessage="this-field-is-required-and-must-contain-only-following-characters" name="custom">
						function(val, fieldNode, ruleValue) {
							var allowedCharacters = '<%= HtmlUtil.escapeJS(LDFPortletKeys.ALLOWED_SITE_NAME) %>';
							val = val.trim();
							var regex = new RegExp('[^' + allowedCharacters + ']');
							var sufixcheck = new RegExp('LFR_ORGANIZATION');
							return !regex.test(val) && !sufixcheck.test(val);
						}
					</aui:validator>				
					<aui:validator name="required" />				
				</aui:input>
		
				<aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary"/>
			</aui:form>	
		</aui:fieldset>
	</aui:fieldset-group>
</div>
