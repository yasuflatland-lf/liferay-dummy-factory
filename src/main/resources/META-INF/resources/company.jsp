<%@ include file="/init.jsp"%>
<%@ page contentType="text/html; charset=UTF-8" %>

<clay:navigation-bar
	inverted="<%= true %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Company") %>'
/>

<div class="container-fluid-1280">

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
		
			<liferay-ui:success key="success" message="Companies created successfully" />
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.COMPANY %>" var="companyEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_COMPANY %>" />
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>		
            <div class="entry-title form-group">
                <h1>Create Companies&nbsp;&nbsp;
                    <a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
                    </a>
                </h1>
            </div>
        
            <div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
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

			<aui:form action="<%= companyEditURL %>" method="post" name="fm" >
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
			
			<liferay-ui:upload-progress
				id="<%= progressId %>"
				message="creating..."
			/>	
						
		</aui:fieldset>
	</aui:fieldset-group>
</div>

<aui:script use="aui-base">
	// Generate dummy data
	$('#<portlet:namespace />processStart').on(
	    'click',
	    function() {
	    	event.preventDefault();
			<%= progressId %>.startProgress();
			submitForm(document.<portlet:namespace />fm);
	    }
	)
	
</aui:script>