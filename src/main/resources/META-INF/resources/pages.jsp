<%@ include file="/init.jsp" %>

<clay:navigation-bar
        inverted="<%= false %>"
        navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Pages") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">
    <div class="sheet">
        <div class="panel-group panel-group-flush">
            <aui:fieldset>

                <liferay-ui:success key="success" message="Pages created successfully"/>
                <%@ include file="/command_select.jspf" %>

                <portlet:actionURL name="<%= LDFPortletKeys.PAGES %>" var="pageEditURL">
                    <portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_PAGES %>"/>
                    <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
                </portlet:actionURL>

                <div id="<portlet:namespace />Header0" role="tab">
                    <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                         class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                         href="#<portlet:namespace />Collapse0" role="button">
                        <h1>Create Pages <liferay-ui:icon-help message="usage"/></h1>
                    </div>
                </div>

                <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                     class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                    <blockquote class="blockquote-info">
                        <small>Example</small>
                        <p>if you enter the values <code>3</code> and "page" the portlet will create three pages: <code>page1</code>,
                            <code>page2</code>, and <code>page3</code>.
                        <p>
                    </blockquote>

                    <ul>
                        <li>You must be signed in as an administrator in order to create pages</li>
                        <li>The counter always starts at <code>1</code></li>
                        <li>If no site is selected, the default site will be <code>liferay.com</code></li>
                    </ul>
                </div>

                <%
                    String numberOfPagesLabel = "Enter the number of pages you would like to create";
                    String basePageNameLabel = "Enter the base page name (i.e. newPage, page, testPage)";
                    List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                    String defaultOption = "(None)";

                    String scopeGroupdId = String.valueOf(themeDisplay.getScopeGroupId());
                    String groupLabel = "Select a site to assign the pages to";
                    String pageLabel = "Select a parent page";
                %>

                <aui:form action="<%= pageEditURL %>" method="post" name="fm"
                          onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                    <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>

                    <aui:input name="numberOfpages" label="<%= numberOfPagesLabel %>">
                        <aui:validator name="digits"/>
                        <aui:validator name="min">1</aui:validator>
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:input name="basePageName" label="<%= basePageNameLabel %>">
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:select name="group" label="<%= groupLabel %>">
                        <aui:option label="<%= defaultOption %>" value="<%= scopeGroupdId %>"/>
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


                    <%
                        String parentLayoutIdLabel = "Enter the parent page ID";
                        String privateLayoutLabel = "Make pages private";
                        String hiddenLabel = "Hide from Navigation Menu";
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

                                <aui:select name="parentLayoutId" label="<%=pageLabel %>">
                                    <aui:option label="<%= defaultOption %>"
                                                value="<%= LayoutConstants.DEFAULT_PARENT_LAYOUT_ID %>"/>
                                </aui:select>
                                <aui:input type="hidden" name="layoutType" label="<%= privateLayoutLabel %>"
                                           value="<%= LayoutConstants.TYPE_PORTLET %>"/>

                            </aui:fieldset>
                            <aui:fieldset cssClass="col-md-6">
                                <aui:input type="toggle-switch" name="privateLayout" label="<%= privateLayoutLabel %>"
                                           value="false"/>
                                <aui:input type="toggle-switch" name="hidden" label="<%= hiddenLabel %>" value="false"/>
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

<portlet:resourceURL id="<%=LDFPortletKeys.CMD_PAGES_FOR_A_SITE %>" var="pagesForASiteURL"/>

<script type="text/html" id="<portlet:namespace />page_per_site_options">
    <option value="<@= parentLayoutId @>"><@= name @></option>
</script>

<aui:script use="aui-base,liferay-form">
    // Ajax
    function ajax(cmd, path, data, handler) {
        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function() {
            if (xmlhttp.readyState == XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                if (xmlhttp.status == 200) {
                    var jsonData = JSON.parse(xmlhttp.response);
                    handler(jsonData)
                }
                else {
                    console.error('status: ' + xmlhttp.status);
                }
            }
        };

        xmlhttp.open(cmd, path, true);
        xmlhttp.send(data);
    }

    document.getElementById("<portlet:namespace/>group")
        .addEventListener("change", function(event) {
            var data = Liferay.Util.ns(
                '<portlet:namespace/>',
                {
            <%=Constants.CMD %>: '<%=PageMVCResourceCommand.CMD_PAGELIST%>',
                siteGroupId: $('#<portlet:namespace/>group').val()
                }
            );

            ajax("POST", '<%= pagesForASiteURL.toString() %>', data, function(dataIn) {
                var data = dataIn;

                Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
                    (function() {
                        var _ = _lodash;

                        //Load Template
                        var tmpl = _.template(document.getElementById('<portlet:namespace/>page_per_site_options').innerHTML);
                        var listAll =
                            tmpl({
                                name:'(None)',
                                parentLayoutId:0
                            });
                        _.map(data,function(n) {
                            listAll +=
                                tmpl(
                                    {
                                        name:(n.name) ? _.escape(n.name) : "",
                                        parentLayoutId:(n.parentLayoutId) ? _.escape(n.parentLayoutId) : ""
                                    }
                                );
                        });
                        var pageObj = document.getElementById('<portlet:namespace/>parentLayoutId');
                        pageObj.empty();
                        pageObj.append(listAll);

                    })()
                }, function(error) {
                    console.error(error)
                });
            });
        });
</aui:script>
