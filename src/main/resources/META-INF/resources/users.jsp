<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.USERS %>" var="userEditURL">
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
            <div class="entry-title form-group">
                <h1>Create Users&nbsp;&nbsp;
                    <a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
                    </a>
                </h1>
            </div>
        
            <div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
                <blockquote class="blockquote-info">
                    <small>Example</small>
                    <p>if you enter the values <code>3</code> and <code>user</code> the portlet will create three blank sites: <code>user1</code>, <code>user2</code>, and <code>user3</code>.<p>
                </blockquote>
            
                <ul>
                    <li>You must be signed in as an administrator in order to create users</li>
                    <li>The counter always starts at <code>1</code></li>
					<li>Email addresses will be the base screenName + "@liferay.com"</li>
					<li>Passwords are set to <code>test</code></li>
					<li>Users' first names will be the base screenName you input</li>
					<li>Users' last names will be the counter</li>                
                </ul>
            
                <h3>Creating Large Batches of Users</h3>
                <ul>
                    <li>If the number of users is large (over <code>100</code>), go to <i>Control Panel -> Server Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code> and set to <code>INFO</code> to track progress (batches of 10%)</li>
                    <li>It may take some time (even for the logs to show) to create a large number of users, and the page will hang until the process is complete; you can query the database if you are uncertain of the progress</li>
                </ul>
            </div>

			<%
			String numberOfusersLabel= "Enter the number of users you would like to create";
			String baseScreenNameLabel= "Enter the base screenName for the users (i.e. newUser, testUser, user)";
			
			%>

			<aui:form action="<%= userEditURL %>" method="post" >
				<aui:input name="numberOfusers" label="<%= numberOfusersLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseScreenName" label="<%= baseScreenNameLabel %>" >
					<aui:validator name="required" />				
				</aui:input>
		
				<%
				String defaultOption = "(None)";

				//Organization
				List<Organization> organizations = OrganizationLocalServiceUtil.getOrganizations(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
				List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
				List<Role> roles = RoleLocalServiceUtil.getRoles(company.getCompanyId());
				List<UserGroup> userGroups = UserGroupLocalServiceUtil.getUserGroups(company.getCompanyId());
				Role defaultRole = RoleLocalServiceUtil.getRole(company.getCompanyId(), RoleConstants.USER);	
				String scopeGroupdId = String.valueOf(themeDisplay.getScopeGroupId());

				String organizationLabel = "Select organizations to assign the users to";
				String groupLabel = "Select sites to assign the users to";
				String roleLabel = "Select roles to assign the users to";
				String roleHelpMessage = "Organization and site roles cannot be assigned unless users are assigned to an organization or site.";
				String userGroupsLabel = "Select user groups to assign the users to";
				String passwordLabel = "Enter password";
				String maleLabel = "Genger (checked is male)";

				%>
		
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-6">
							<aui:select name="organizations" label="<%= organizationLabel %>" multiple="<%= true %>">
								<%
								for (Organization organization : organizations) {
								%>
									<aui:option label="<%= organization.getName() %>" value="<%= organization.getOrganizationId() %>"/>
								<%
								}
								%>
							</aui:select>	
							<aui:select name="groups" label="<%= groupLabel %>"  multiple="<%= true %>" >
								<%
								for (Group group : groups) {
									if (group.isSite() && !group.getDescriptiveName().equals("Control Panel")) {
								%>
										<aui:option label="<%= group.getDescriptiveName() %>" value="<%= group.getGroupId() %>"/>
								<%
									}
								}
								%>
							</aui:select>		
							<aui:select name="roles" label="<%= roleLabel %>" helpMessage="<%= roleHelpMessage %>"  multiple="<%= true %>" >
								<%
								for (Role role : roles) {
								%>
									<aui:option label="<%= role.getDescriptiveName() %>" value="<%= role.getRoleId() %>"/>
								<%
								}
								%>
							</aui:select>
											
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input name="password" label="<%= passwordLabel %>" value="test"/>
							<aui:input name="male" label="<%= maleLabel %>" value="<%= true %>"/>		
							<aui:select name="userGroups" label="<%= userGroupsLabel %>"  multiple="<%= true %>" >
								<%
								for (UserGroup userGroup : userGroups) {
								%>
									<aui:option label="<%= userGroup.getName() %>" value="<%= userGroup.getUserGroupId() %>"/>
								<%
								}
								%>
							</aui:select>
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
