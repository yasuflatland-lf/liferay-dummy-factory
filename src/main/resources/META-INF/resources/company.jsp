<%@ include file="/init.jsp"%>
<%@ page contentType="text/html; charset=UTF-8" %>

<clay:navigation-bar
	inverted="<%= false %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Company") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
		
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.COMPANY %>" var="companyEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_COMPANY %>" />
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>

			<div id="<portlet:namespace />Header0" role="tab">
				<div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
					 class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
					 href="#<portlet:namespace />Collapse0" role="button">
					<h1>Create Companies <liferay-ui:icon-help message="usage" /></h1>
				</div>
			</div>

			<div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
				 class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
				<blockquote class="blockquote-info">
					<small>Example</small>
					<p>if you enter the values <code>3</code> and <code>company</code> the portlet will create three companies: <code>company1</code>, <code>company2</code>, and <code>company3</code>.<p>
				</blockquote>

				<p>You must be signed in as an administrator in order to create companies</p>
				<p>The counter always starts at <code>1</code></p>
				<p>If no site is selected, the default site will be <code>liferay.com</code></p>
				<p><code>Dummy Contents Create</code> mode will use the dummy data generation. For the configuration of the dummy image resources, Navigate to "Organization" panel, and elipse button is displayed on the top right.</p>
			</div>

			<%
			String numberOfCompaniesLabel= "Enter the number of companies you would like to create";
			String webIdLabel= "Enter the Web ID";
			String virtualHostnameLabel = "Enter the Virtual Host Name";
			String mxLabel = "Enter the Mail Domain";
			String maxUsersLabel = "Max user numbers";
			String activeLabel = "Activate the company";
			%>

			<aui:form action="<%= companyEditURL %>" method="post" name="fm"  onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
				<liferay-ui:success key="success" message="Companies created successfully" />
				<liferay-ui:error exception="<%= Exception.class %>" message="Error occured. Please see console log" />
				<liferay-ui:error exception="<%= CompanyMxException.class %>" message="please-enter-a-valid-mail-domain" />
				<liferay-ui:error exception="<%= CompanyVirtualHostException.class %>" message="please-enter-a-valid-virtual-host" />
				<liferay-ui:error exception="<%= CompanyWebIdException.class %>" message="please-enter-a-valid-web-id" />

				<aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>
			
				<aui:input name="numberOfCompanies" label="<%= numberOfCompaniesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				
				<aui:input name="webId" label="<%= webIdLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="virtualHostname" label="<%= virtualHostnameLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="mx" label="<%= mxLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>

				<aui:input name="maxUsers" label="<%= maxUsersLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>

				<aui:input name="active" type="toggle-switch" label="<%= activeLabel %>" value="<%= true %>"/>
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

<aui:script>
	function <portlet:namespace />execCommand() {
		<%= progressId %>.startProgress();
		submitForm(document.<portlet:namespace />fm);
	}
</aui:script>