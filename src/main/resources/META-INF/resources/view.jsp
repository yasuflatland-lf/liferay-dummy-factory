<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
	<liferay-ui:success key="success" message="Organizations created successfully" />
		
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.ORGANIZAION %>" var="organizationEditURL">
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>

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

			<%
			String numberOfOrganizationsLabel = "Enter the number of organizations you would like to create";
			String baseOrganizationNameLabel = "Enter the base name for the organizations";
			String parentOrganizationIdLabel = "Enter the parent organization ID";
			%>

			<aui:form action="<%= organizationEditURL %>" method="post" >
				<aui:input name="numberOfOrganizations" label="<%= numberOfOrganizationsLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseOrganizationName" label="<%= baseOrganizationNameLabel %>" >
					<aui:validator name="required" />				
				</aui:input>
		
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-6">
							<aui:input name="parentOrganizationId" label="<%= parentOrganizationIdLabel %>" >
								<aui:validator name="digits" />
							</aui:input>				
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
