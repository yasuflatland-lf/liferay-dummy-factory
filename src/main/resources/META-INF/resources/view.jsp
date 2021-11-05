<%@ include file="/init.jsp" %>

<clay:navigation-bar
        inverted="<%= true %>"
        navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Organization") %>'
/>

<div class="container-fluid-1280">

    <aui:fieldset-group markupView="lexicon">
        <aui:fieldset>

            <liferay-ui:success key="success" message="Organizations created successfully"/>
            <%@ include file="/command_select.jspf" %>

            <portlet:actionURL name="<%= LDFPortletKeys.ORGANIZAION %>" var="organizationEditURL">
                <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
            </portlet:actionURL>

            <div id="<portlet:namespace />Header0" role="tab">
                <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                     class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                     href="#<portlet:namespace />Collapse0" role="button">
                    <h1>Create Organizations <liferay-ui:icon-help message="usage" /></h1>
                </div>
            </div>

            <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                 class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                <blockquote class="blockquote-info">
                    <small>Example</small>
                    <p>If you enter the values <code>3</code> and "org" the portlet will create three organizations:
                        <code>org1</code>, <code>org2</code>, and <code>org3</code>.
                    <p>
                </blockquote>
                <ul>
                    <li>You must be signed in as an administrator in order to create organizations</li>
                    <li>The counter always starts at <code>1</code></li>
                    <li>The organization type is <code>Organization</code></li>
                </ul>
            </div>

            <%
                String numberOfOrganizationsLabel = "Enter the number of organizations you would like to create";
                String baseOrganizationNameLabel = "Enter the base name for the organizations";
                String parentOrganizationIdLabel = "Select the parent organization";
                String organizationSiteCreateLabel = "Creating organization site";
            %>

            <aui:form action="<%= organizationEditURL %>" method="post" name="fm"
                      onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>

                <aui:input name="<%= Constants.CMD %>" type="hidden"/>
                <aui:input name="redirect" type="hidden" value="<%= redirect %>"/>
                <aui:input name="numberOfOrganizations" label="<%= numberOfOrganizationsLabel %>">
                    <aui:validator name="digits"/>
                    <aui:validator name="min">1</aui:validator>
                    <aui:validator name="required"/>
                </aui:input>
                <aui:input name="baseOrganizationName" label="<%= baseOrganizationNameLabel %>">
                    <aui:validator name="required"/>
                </aui:input>

                <%
                    //Organization
                    List<Organization> organizations = OrganizationLocalServiceUtil.getOrganizations(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                    String defaultOption = "(None)";
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

                    <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header"
                         class="collapse panel-collapse" id="<portlet:namespace />Collapse" role="tabpanel">
                        <div class="simulation-app-panel-body">
                            <div class="row">
                                <aui:fieldset cssClass="col-md-6">
                                    <aui:select name="parentOrganizationId" label="<%= parentOrganizationIdLabel %>">
                                        <aui:option label="<%= defaultOption %>"
                                                    value="<%= OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID %>"/>
                                        <%
                                            for (Organization organization : organizations) {
                                        %>
                                        <aui:option label="<%= organization.getName() %>"
                                                    value="<%= organization.getOrganizationId() %>"/>
                                        <%
                                            }
                                        %>
                                    </aui:select>
                                </aui:fieldset>
                                <aui:fieldset cssClass="col-md-6">
                                    <aui:input type="toggle-switch" name="organizationSiteCreate"
                                               label="<%= organizationSiteCreateLabel %>" value="false"/>
                                </aui:fieldset>
                            </div>
                        </div>
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

