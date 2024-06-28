<%@ include file="/init.jsp" %>

<clay:navigation-bar
        inverted="<%= false %>"
        navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Users") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">
    <div class="sheet">
        <div class="panel-group panel-group-flush">
            <aui:fieldset>

                <liferay-ui:success key="success" message="Users created successfully"/>
                <liferay-ui:error key="group-friendly-url-error"
                                  message="The username has already been used for the name of a site or an organization. These names must be unique."/>

                <%@ include file="/command_select.jspf" %>

                <portlet:actionURL name="<%= LDFPortletKeys.USERS %>" var="userEditURL">
                    <portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_USERS %>"/>
                    <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
                </portlet:actionURL>

                <div id="<portlet:namespace />Header0" role="tab">
                    <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                         class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                         href="#<portlet:namespace />Collapse0" role="button">
                        <h1>Create Users <small><liferay-ui:icon-help message="usage"/></small></h1>
                    </div>
                </div>

                <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                     class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                    <div class="alert alert-info">
                        <h4>Example</h4>
                        <p>if you enter the values <code>3</code> and <code>user</code> the portlet will create three users:
                            <code>user1</code>, <code>user2</code>, and <code>user3</code>.
                        <p>
                        <hr class="separator" />
                        <ul>
                            <li>You must be signed in as an administrator in order to create users</li>
                            <li>The counter always starts at <code>1</code></li>
                            <li>Email addresses will be the base screenName + "@liferay.com"</li>
                            <li>Passwords are set to <code>test</code></li>
                            <li>Users' first names will be the base screenName you input</li>
                            <li>Users' last names will be the counter</li>
                        </ul>

                        <h5>Creating Large Batches of Users</h5>
                        <ul>
                            <li>If the number of users is large (over <code>100</code>), go to <i>Control Panel -> Server
                                Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code>
                                and set to <code>INFO</code> to track progress (batches of 10%)
                            </li>
                            <li>It may take some time (even for the logs to show) to create a large number of users, and the
                                page will hang until the process is complete; you can query the database if you are uncertain of
                                the progress
                            </li>
                        </ul>
                    </div>
                </div>

                <%
                    String numberOfusersLabel = "Enter the number of users you would like to create";
                    String baseScreenNameLabel = "Enter the base screenName for the users (i.e. newUser, testUser, user)";
                    String baseDomainLabel = "Domain name here. (i.e. liferay.com, gmail.com). If no domain specified, liferay.com will be used.";

                %>

                <aui:form action="<%= userEditURL %>" method="post" name="fm"
                          onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                    <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>
                    <aui:input name="numberOfusers" label="<%= numberOfusersLabel %>">
                        <aui:validator name="digits"/>
                        <aui:validator name="min">1</aui:validator>
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:input name="baseScreenName" label="<%= baseScreenNameLabel %>">
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:input name="baseDomain" label="<%= baseDomainLabel %>" value="liferay.com"/>

                    <%
                        String defaultOption = "(None)";

                        //Organization
                        List<Organization> organizations = OrganizationLocalServiceUtil.getOrganizations(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                        List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                        List<Role> roles = RoleLocalServiceUtil.getRoles(company.getCompanyId());
                        List<UserGroup> userGroups = UserGroupLocalServiceUtil.getUserGroups(company.getCompanyId());

                        String organizationLabel = "Select organizations to assign the users to";
                        String groupLabel = "Select sites to assign the users to";
                        String roleLabel = "Select roles to assign the users to";
                        String roleHelpMessage = "Organization and site roles cannot be assigned unless users are assigned to an organization or site.";
                        String userGroupsLabel = "Select user groups to assign the users to";
                        String passwordLabel = "Enter password";
                        String maleLabel = "Genger (checked is male)";
                        String fakerEnableLabel = "Use Real names for First and Last name (Generated by Faker)";
                        String localesLabel = "Locale for Autogenerated names";
                        String autoUserPreLoginLabel = "Force users to login right after the user creation";

                    %>

                    <liferay-frontend:fieldset collapsed="<%= true %>" collapsible="<%= true %>" label="CLICK HERE TO SHOW MORE OPTIONS">

                        <div class="form-group row">
                            <div class="col-md-12">

                                <liferay-frontend:fieldset cssClass="mb-0" collapsed="<%= true %>" collapsible="<%= true %>" label="Main Configurations">
                                    <div class="col-md-6">

                                        <aui:input type="toggle-switch" name="autoUserPreLogin"
                                                   label="<%= autoUserPreLoginLabel %>" value="false"/>

                                        <aui:select name="organizations" label="<%= organizationLabel %>"
                                                    multiple="<%= true %>">
                                            <%
                                                for (Organization organization : organizations) {
                                            %>
                                            <aui:option label="<%= organization.getName() %>"
                                                        value="<%= organization.getOrganizationId() %>"/>
                                            <%
                                                }
                                            %>
                                        </aui:select>
                                        <aui:select name="groups" label="<%= groupLabel %>"
                                                    multiple="<%= true %>">
                                            <%
                                                for (Group group : groups) {
                                                    if (group.isSite() && !group.getDescriptiveName().equals("Control Panel")) {
                                            %>
                                            <aui:option label="<%= group.getDescriptiveName() %>"
                                                        value="<%= group.getGroupId() %>"/>
                                            <%
                                                    }
                                                }
                                            %>
                                        </aui:select>
                                        <aui:select name="roles" label="<%= roleLabel %>"
                                                    helpMessage="<%= roleHelpMessage %>"
                                                    multiple="<%= true %>">
                                            <%
                                                for (Role role : roles) {
                                            %>
                                            <aui:option label="<%= role.getDescriptiveName() %>"
                                                        value="<%= role.getPrimaryKey() %>"/>
                                            <%
                                                }
                                            %>
                                        </aui:select>
                                        <aui:select name="userGroups" label="<%= userGroupsLabel %>"
                                                    multiple="<%= true %>">
                                            <%
                                                for (UserGroup userGroup : userGroups) {
                                            %>
                                            <aui:option label="<%= userGroup.getName() %>"
                                                        value="<%= userGroup.getUserGroupId() %>"/>
                                            <%
                                                }
                                            %>
                                        </aui:select>
                                    </div>
                                    <div class="col-md-6">
                                        <aui:input name="password" label="<%= passwordLabel %>"
                                                   value="test"/>
                                        <aui:input name="male" type="toggle-switch" label="<%= maleLabel %>"
                                                   value="<%= true %>"/>
                                        <aui:input name="fakerEnable" type="toggle-switch"
                                                   label="<%= fakerEnableLabel %>" value="<%= false %>"/>
                                        <%
                                            Set<Locale> locales = LanguageUtil.getAvailableLocales(themeDisplay.getSiteGroupId());
                                            //TODO : Locale need to be filtered for only available
                                            //List<Locale> locales = UserDataService.getFakerAvailableLocales(orgLocales);

                                        %>
                                        <aui:select name="locale" label="<%= localesLabel %>"
                                                    multiple="<%= false %>">
                                            <%
                                                for (Locale availableLocale : locales) {
                                            %>
                                            <aui:option
                                                    label="<%= availableLocale.getDisplayName(locale) %>"
                                                    value="<%= availableLocale.getLanguage() %>"
                                                    selected="<%= availableLocale.toString().equals(LocaleUtil.getDefault().toString()) %>"/>
                                            <%
                                                }
                                            %>
                                        </aui:select>

                                        <%
                                            String publicLayoutSetPrototypeIdLabel = "My Profile Site Template";
                                            String privateLayoutSetPrototypeIdLabel = "My Dashboard Site Template";

                                            List<LayoutSetPrototype> layoutSetPrototypes = LayoutSetPrototypeLocalServiceUtil.search(
                                                    themeDisplay.getCompanyId(), true,
                                                    QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
                                        %>
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

                                        <aui:select name="privateLayoutSetPrototypeId"
                                                    label="<%= privateLayoutSetPrototypeIdLabel %>">
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

                                    </div>
                                </liferay-frontend:fieldset>

                            </div>
                        </div>

                        <liferay-frontend:fieldset collapsed="<%= true %>" collapsible="<%= true %>" label="Announcements Deliveries">
                            <div class="panel-body">
                                <%@ include file="/user/announcements.jsp" %>
                            </div>
                        </liferay-frontend:fieldset>
                    </liferay-frontend:fieldset>
                    <aui:button-row>
                        <aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary" id="processStart"/>
                    </aui:button-row>
                </aui:form>
                <%
                    // Because of bug of lifeary-ui:upload-progress, you need to add the following parameter in the request.
                    String progressSessionKey = ProgressTracker.PERCENT + progressId;
                    request.setAttribute("liferay-ui:progress:sessionKey", progressSessionKey);
                %>
                <liferay-document-library:upload-progress
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

        submitForm(document.<portlet:namespace/>fm);
    }
</aui:script>

<portlet:resourceURL id="<%=LDFPortletKeys.CMD_ROLELIST %>" var="roleListURL"/>

<script type="text/html" id="<portlet:namespace />roles_options">
    <option value="<@= roleId @>" data-role-type="<@= type @>"><@= name @></option>
</script>

<aui:script use="aui-base,liferay-form">
    function ajax(cmd, path, data, handler) {
        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                if (xmlhttp.status == 200) {
                    var jsonData = JSON.parse(xmlhttp.response);
                    handler(jsonData)
                } else {
                    console.error('status: ' + xmlhttp.status);
                }
            }
        };

        xmlhttp.open(cmd, path, true);
        xmlhttp.send(data);
    }

    function <portlet:namespace/>updateRoles() {
        var data = Liferay.Util.ns(
            '<portlet:namespace/>',
            {
        <%=Constants.CMD %>:
        '<%=RoleMVCResourceCommand.CMD_ROLELIST%>',
            isSitesSelected
    :
        (null == document.getElementById('<portlet:namespace/>groups').value) ? false : true,
            isOrganizationSelected
    :
        (null == document.getElementById('<portlet:namespace/>organizations').value) ? false : true
    })
        ;

        ajax("POST", '<%= roleListURL.toString() %>', data, function (dataIn) {
            var data = dataIn;

            Liferay.Loader.require("<%=lodashResolver %>", function (_lodash) {
                (function () {
                    var _ = _lodash;

                    //Load Template
                    var tmpl = _.template(document.getElementById('<portlet:namespace/>roles_options').innerHTML);
                    var listAll = "";
                    _.map(data, function (n) {
                        listAll +=
                            tmpl(
                                {
                                    name: (n.name) ? _.escape(n.name) : "",
                                    roleId: (n.roleId) ? _.escape(n.roleId) : "",
                                    type: (n.type) ? _.escape(n.type) : ""
                                }
                            );
                    });
                    var pageObj = document.getElementById('<portlet:namespace/>roles')
                    pageObj.empty();
                    pageObj.append(listAll);

                })()
            }, function (error) {
                console.error(error)
            });
        });

        document.createElement("<portlet:namespace/>organizations")
            .addEventListener("change", function (event) {
                <portlet:namespace/>updateRoles();
            });

        document.createElement("<portlet:namespace/>groups")
            .addEventListener("change", function (event) {
                <portlet:namespace/>updateRoles();
            });

        document.addEventListener('DOMContentLoaded', function () {
            //Initialize role options
            <portlet:namespace/>updateRoles();
        });
    }
</aui:script>
