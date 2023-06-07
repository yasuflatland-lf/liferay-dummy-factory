<%@ include file="/init.jsp" %>

<clay:navigation-bar
        inverted="<%= false %>"
        navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Sites") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">
    <div class="sheet">
        <div class="panel-group panel-group-flush">
            <aui:fieldset>

                <liferay-ui:success key="success" message="Sites created successfully"/>
                <%@ include file="/command_select.jspf" %>

                <portlet:actionURL name="<%= LDFPortletKeys.SITES %>" var="siteEditURL">
                    <portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_SITES %>"/>
                    <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
                </portlet:actionURL>

                <div id="<portlet:namespace />Header0" role="tab">
                    <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                         class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                         href="#<portlet:namespace />Collapse0" role="button">
                        <h1>Create Sites <small><liferay-ui:icon-help message="usage"/></small></h1>
                    </div>
                </div>

                <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                     class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                    <div class="alert alert-info">
                        <h4>Example</h4>
                        <p>if you enter the values <code>3</code> and "site" the portlet will create three blank sites:
                            <code>site1</code>, <code>site2</code>, and <code>site3</code>.
                        <p>
                        <hr class="separator" />

                        <ul>
                            <li>You must be signed in as an administrator in order to create sites</li>
                            <li>The counter always starts at <code>1</code></li>
                            <li>The site type is <code>Blank Site</code></li>
                        </ul>
                    </div>

                    <h3>Creating Large Batches of Sites</h3>
                    <ul>
                        <li>If the number of sites is large (over <code>100</code>), go to <i>Control Panel -> Server
                            Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code>
                            and set to <code>INFO</code> to track progress (batches of 10%)
                        </li>
                        <li>It may take some time (even for the logs to show) to create a large number of sites, and the
                            page will hang until the process is complete; you can query the database if you are uncertain of
                            the progress
                        </li>
                    </ul>
                </div>

                <%
                    String numberOfSitesLabel = "Enter the number of sites you would like to create";
                    String baseSiteNameLabel = "Enter the base name for the sites";
                    String siteTypeLabel = "Site type";
                %>

                <aui:form action="<%= siteEditURL %>" method="post" name="fm"
                          onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                    <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>

                    <aui:select name="siteType" label="<%=siteTypeLabel %>">
                        <aui:option label="<%=GroupConstants.TYPE_SITE_OPEN_LABEL %>"
                                    value="<%= GroupConstants.TYPE_SITE_OPEN %>"/>
                        <aui:option label="<%=GroupConstants.TYPE_SITE_PRIVATE_LABEL %>"
                                    value="<%= GroupConstants.TYPE_SITE_PRIVATE %>"/>
                        <aui:option label="<%=GroupConstants.TYPE_SITE_RESTRICTED_LABEL %>"
                                    value="<%= GroupConstants.TYPE_SITE_RESTRICTED %>"/>
                    </aui:select>
                    <aui:input name="numberOfSites" label="<%= numberOfSitesLabel %>">
                        <aui:validator name="digits"/>
                        <aui:validator name="min">1</aui:validator>
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:input name="baseSiteName" label="<%= baseSiteNameLabel %>">
                        <aui:validator name="required"/>
                    </aui:input>

                    <%
                        String parentGroupIdLabel = "Select the parent site";
                        String manualMembershipLabel = "Enable manual membership";
                        String inheritContentLabel = "Enable inherit content";
                        String activeLabel = "Activate site";
                        String publicLayoutSetPrototypeIdLabel = "Site Template";

                        List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                        String defaultOption = "(None)";

                        List<LayoutSetPrototype> layoutSetPrototypes = LayoutSetPrototypeLocalServiceUtil.search(
                                themeDisplay.getCompanyId(), true,
                                QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
                    %>

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
                            <aui:fieldset cssClass="col-md-6">
                                <aui:select name="parentGroupId" label="<%= parentGroupIdLabel %>">
                                    <aui:option label="<%= defaultOption %>"
                                                value="<%= GroupConstants.DEFAULT_PARENT_GROUP_ID %>"/>
                                    <%
                                        for (Group group : groups) {
                                            if (group.isSite()) {
                                    %>
                                    <aui:option label="<%= group.getDescriptiveName() %>"
                                                value="<%= group.getGroupId() %>"/>
                                    <%
                                            }
                                        }
                                    %>
                                </aui:select>

                                <aui:select name="publicLayoutSetPrototypeId"
                                            label="<%= publicLayoutSetPrototypeIdLabel %>">
                                    <aui:option label="<%= defaultOption %>" value="<%= 0 %>"/>
                                    <%
                                        for (LayoutSetPrototype layoutSetPrototype : layoutSetPrototypes) {
                                    %>
                                    <aui:option label="<%= layoutSetPrototype.getName(locale) %>"
                                                value="<%= layoutSetPrototype.getLayoutSetPrototypeId() %>"/>
                                    <%
                                        }
                                    %>
                                </aui:select>
                            </aui:fieldset>
                            <aui:fieldset cssClass="col-md-6">
                                <aui:input type="toggle-switch" name="manualMembership" label="<%= manualMembershipLabel %>"
                                           value="true"/>
                                <aui:input type="toggle-switch" name="inheritContent" label="<%= inheritContentLabel %>"
                                           value="false"/>
                                <aui:input type="toggle-switch" name="active" label="<%= activeLabel %>" value="true"/>
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
        </div>
    </div>

</div>


<aui:script>
    function <portlet:namespace/>execCommand() {
    <%= progressId %>.startProgress();
    submitForm(document.<portlet:namespace/>fm);
    }
</aui:script>