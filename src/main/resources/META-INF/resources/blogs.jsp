
<%@ include file="/init.jsp"%>

<clay:navigation-bar
	inverted="<%= true %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Blogs") %>'
/>

<div class="container-fluid-1280">

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
		
			<liferay-ui:success key="success" message="Blogs created successfully" />
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.BLOGS %>" var="blogsEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_BLOGS %>" />
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>		
            <div class="entry-title form-group">
                <h1>Create Blog posts&nbsp;&nbsp;
                    <a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
                    </a>
                </h1>
            </div>
        
            <div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
                <blockquote class="blockquote-info">
                    <small>Example</small>
                    <p>if you enter the values <code>3</code> and <code>blog</code> the portlet will create three blog posts: <code>blog1</code>, <code>blog2</code>, and <code>blog3</code>.<p>
                </blockquote>
            
                <p>You must be signed in as an administrator in order to create blog posts</p>
                <p>The counter always starts at <code>1</code></p>
				<p>If no site is selected, the default site will be <code>liferay.com</code></p>
            </div>

			<%
			String numberOfPostsLabel= "Enter the number of blog posts you would like to create";
			String groupLabel = "Group ID";
			String baseTitleLabel= "Enter the base title";
			String contentsLabel = "Enter the contents";
			String userIdLabel = "User ID";
			String defaultOption = "None";
			String allowPingbacksLabel = "Arrow Pingbacks";
			String allowTrackbacksLabel = "Please enter trackback address by comma separated strings";
			
			List<User> users = UserLocalServiceUtil.getCompanyUsers(themeDisplay.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			final String groupName = GroupConstants.GUEST;
			final long companyId = PortalUtil.getDefaultCompanyId();
			final long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);

			%>

			<aui:form action="<%= blogsEditURL %>" method="post" name="fm" >
				<aui:input name="numberOfPosts" label="<%= numberOfPostsLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="groupId" label="<%= groupLabel %>" >
					<aui:option label="<%= defaultOption %>" value="<%= guestGroupId %>" />
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
				
				<aui:input name="baseTitle" label="<%= baseTitleLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="contents" label="<%= contentsLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="userId" label="<%= userIdLabel %>" multiple="<%= false %>" >
					<aui:option label="<%= user.getFullName() %>" value="<%= user.getUserId() %>" selected="true"/>
					<%
					for (User listUser : users) {
						if(listUser.getUserId() != user.getUserId()) {
					%>
						<aui:option label="<%= listUser.getFullName() %>" value="<%= listUser.getUserId() %>"/>
					<%
						}
					}
					%>
				</aui:select>		

				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
				
					<div class="row">
						<aui:fieldset cssClass="col-md-12">
							<aui:input name="allowPingbacks" type="toggle-switch" label="<%= allowPingbacksLabel %>" value="<%= true %>"/>
							<aui:input name="allowTrackbacks" label="<%= allowTrackbacksLabel %>" cssClass="lfr-textarea-container" rows="10" type="textarea" />
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