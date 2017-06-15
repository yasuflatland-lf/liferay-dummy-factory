<%@ include file="/init.jsp"%>

<%
	PortletURL portletURL = PortletURLUtil.clone(renderResponse.createRenderURL(), liferayPortletResponse);
	String redirect = ParamUtil.getString(request, "redirect");
%>

<div class="container-fluid-1280">
	<h2>What kind of data do you want to create?</h2>
	<aui:select name="command" label=""> 
		<option>Organizations</option>
		<option>Sites</option>
		<option>Pages</option>
		<option>Users</option>
		<option>Web Content Articles</option>
		<option>Documents</option>				
	</aui:select>
	
	<portlet:actionURL name="/samplesb/crud" var="samplesbEditURL">
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>


	<%
	String numberOfOrganizationsLabel = "Enter the number of organizations you would like to create";
	String baseOrganizationNameLabel = "Enter the base name for the organizations";
	%>
	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
			<div class="entry-title form-group">
				<h1>Create organizations&nbsp;&nbsp;
					<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
					</a>
				</h1>
			</div>
		
			<div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
				<blockquote class="blockquote-info">
				    <small>Example</small>
					<p>If you enter the values <code>3</code> and "org" the portlet will create three regular organizations: <code>org1</code>, <code>org2</code>, and <code>org3</code>.<p>
				</blockquote>
			
				<ul>
					<li>You must be signed in as an administrator in order to create organizations</li>
					<li>The counter always starts at <code>1</code></li>
					<li>The organization type is <code>Regular Organization</code></li>
				</ul>
			
				<h3>Creating Large Batches of Organizations</h3>
				<ul>
					<li>If the number of organizations is large (over <code>100</code>), go to <i>Control Panel -> Server Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code> and set to <code>INFO</code> to track progress (batches of 10%)</li>
					<li>It may take some time (even for the logs to show) to create a large number of organizations, and the page will hang until the process is complete; you can query the database if you are uncertain of the progress</li>
				</ul>
			</div>

			<aui:form action="<%= samplesbEditURL %>" method="post" >
				<aui:input name="companyId" type="hidden" value="<%= company.getCompanyId() %>" />
				<aui:input name="numberOfOrganizations" label="<%= numberOfOrganizationsLabel %>" /><br />
				<aui:input name="baseOrganizationName" label="<%= baseOrganizationNameLabel %>" /><br />
		
				<aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary"/>
			</aui:form>	
		</aui:fieldset>
	</aui:fieldset-group>
</div>
