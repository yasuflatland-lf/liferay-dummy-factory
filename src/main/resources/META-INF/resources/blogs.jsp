<%@ include file="/init.jsp" %>

<clay:navigation-bar
        inverted="<%= true %>"
        navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Blogs") %>'
/>

<div class="container-fluid-1280">

    <aui:fieldset-group markupView="lexicon">
        <aui:fieldset>

            <liferay-ui:success key="success" message="Blogs created successfully"/>
            <%@ include file="/command_select.jspf" %>

            <portlet:actionURL name="<%= LDFPortletKeys.BLOGS %>" var="blogsEditURL">
                <portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_BLOGS %>"/>
                <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
            </portlet:actionURL>

            <div id="<portlet:namespace />Header0" role="tab">
                <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                     class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                     href="#<portlet:namespace />Collapse0" role="button">
                    <h1>Create Blog posts <liferay-ui:icon-help message="usage" /></h1>
                </div>
            </div>

            <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                 class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                <blockquote class="blockquote-info">
                    <small>Example</small>
                    <p>if you enter the values <code>3</code> and <code>blog</code> the portlet will create three blog
                        posts: <code>blog1</code>, <code>blog2</code>, and <code>blog3</code>.
                    <p>
                </blockquote>

                <p>You must be signed in as an administrator in order to create blog posts</p>
                <p>The counter always starts at <code>1</code></p>
                <p>If no site is selected, the default site will be <code>liferay.com</code></p>
            </div>

            <%
                String numberOfPostsLabel = "Enter the number of blog posts you would like to create";
                String groupLabel = "Group ID";
                String baseTitleLabel = "Enter the base title";
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

            <aui:form action="<%= blogsEditURL %>" method="post" name="fm"
                      onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>
                <aui:input name="numberOfPosts" label="<%= numberOfPostsLabel %>">
                    <aui:validator name="digits"/>
                    <aui:validator name="min">1</aui:validator>
                    <aui:validator name="required"/>
                </aui:input>
                <aui:select name="groupId" label="<%= groupLabel %>">
                    <aui:option label="<%= defaultOption %>" value="<%= guestGroupId %>"/>
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

                <aui:input name="baseTitle" label="<%= baseTitleLabel %>" cssClass="lfr-textarea-container">
                    <aui:validator name="required"/>
                </aui:input>
                <aui:input name="contents" label="<%= contentsLabel %>" cssClass="lfr-textarea-container">
                    <aui:validator name="required"/>
                </aui:input>
                <aui:select name="userId" label="<%= userIdLabel %>" multiple="<%= false %>">
                    <aui:option label="<%= user.getFullName() %>" value="<%= user.getUserId() %>" selected="true"/>
                    <%
                        for (User listUser : users) {
                            if (listUser.getUserId() != user.getUserId()) {
                    %>
                    <aui:option label="<%= listUser.getFullName() %>" value="<%= listUser.getUserId() %>"/>
                    <%
                            }
                        }
                    %>
                </aui:select>

                <div class="panel panel-secondary">
                    <div class="panel-header panel-heading" id="<portlet:namespace />Header" role="tab">
                        <div class="panel-title">
                            <div aria-controls="<portlet:namespace />Collapse" aria-expanded="false"
                                 class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                                 href="#<portlet:namespace />Collapse" role="button">
                                <span class="category-name text-truncate">Click here to show more options</span>
                                <aui:icon cssClass="collapse-icon-closed" image="angle-right" markupView="lexicon"/>
                                <aui:icon cssClass="collapse-icon-open" image="angle-down" markupView="lexicon"/>
                            </div>
                        </div>
                    </div>
                </div>

                <div aria-expanded="false" aria-labelledby="<portlet:namespace/>Header"
                     class="collapse panel-collapse" id="<portlet:namespace/>Collapse" role="tabpanel">
                    <div class="row">
                        <aui:fieldset cssClass="col-md-12">
                            <aui:input name="allowPingbacks" type="toggle-switch" label="<%= allowPingbacksLabel %>"
                                       value="<%= true %>"/>
                            <aui:input name="allowTrackbacks" label="<%= allowTrackbacksLabel %>"
                                       cssClass="lfr-textarea-container" rows="10" type="textarea"/>
                        </aui:fieldset>
                    </div>
                </div>

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
    function <portlet:namespace/>execCommand() {
    <%= progressId %>.startProgress();
    submitForm(document.<portlet:namespace/>fm);
    }
</aui:script>