<%@ include file="/init.jsp" %>

<clay:navigation-bar
    inverted="<%= false %>"
    navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Category / Vocabulary") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">

    <div class="sheet">
        <div class="panel-group panel-group-flush">
            <aui:fieldset>

                <liferay-ui:success key="success" message="Category / Vocabraly created successfully"/>
                <%@ include file="/command_select.jspf" %>

                <portlet:actionURL name="<%= LDFPortletKeys.CATEGORY %>" var="categoryEditURL">
                    <portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_CATEGORY %>"/>
                    <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
                </portlet:actionURL>

                <div id="<portlet:namespace />Header0" role="tab">
                    <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                         class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                         href="#<portlet:namespace />Collapse0" role="button">
                        <h1>Create Category / Vocabulary <liferay-ui:icon-help message="usage" /></h1>
                    </div>
                </div>

                <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                     class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                    <blockquote class="blockquote-info">
                        <small>Example</small>
                        <p>if you enter the values <code>3</code> in the "category / vocabulary", the portlet will create
                            three categories: <code>category1</code>, <code>category2</code>, and <code>category3</code>.
                        <p>
                    </blockquote>

                    <ul>
                        <li>You must be signed in as an administrator in order to create categories / vocabularies</li>
                        <li>The counter always starts at <code>1</code></li>
                        <li>If no site is selected, the default site will be <code>liferay.com</code></li>
                    </ul>
                </div>

                <%
                    String numberOfCategoriesLabel = "Enter the number of categories / vocabralies you would like to create";
                    String baseCategoryNameLabel = "Enter the base Category / Vocabulary name";
                    List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                    final String groupName = GroupConstants.GUEST;
                    final long companyId = PortalUtil.getDefaultCompanyId();
                    final long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();

                    String defaultOption = "(None)";

                    String groupLabel = "Select a site to assign the pages to";
                    String createContentsTypeLabel = "Select create type";
                    String parentCategoryIdLabel = "Enter the parent category ID";
                    String vocabularyIdLabel = "Vocabulary ID";
                %>

                <aui:form action="<%= categoryEditURL %>" method="post" name="fm"
                          onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                    <liferay-ui:error exception="<%= DuplicateCategoryException.class %>"
                                      message="please-enter-a-unique-name"/>

                    <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>

                    <aui:select name="createContentsType" label="<%= createContentsTypeLabel %>">
                        <aui:option label="Category" value="<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %>"/>
                        <aui:option label="Vocabulary" value="<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %>"/>
                    </aui:select>

                    <aui:select name="group" label="<%= groupLabel %>">
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

                    <span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %>"
                          class="<portlet:namespace />contentsTypeGroup">
					<%
                        List<AssetVocabulary> assetVocabularies = AssetVocabularyServiceUtil.getGroupVocabularies(
                                guestGroupId, true);

                    %>

					<aui:select name="vocabularyId" label="<%= vocabularyIdLabel %>">
                        <%
                            for (AssetVocabulary assetVocabulary : assetVocabularies) {
                        %>
                        <aui:option label="<%= assetVocabulary.getName() %>"
                                    value="<%= assetVocabulary.getVocabularyId() %>"/>
                        <%
                            }
                        %>
                    </aui:select>

					<aui:select name="parentCategoryId" label="<%=parentCategoryIdLabel %>">
                        <aui:option value="<%= String.valueOf(AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID) %>"
                                    selected="true">(None)</aui:option>
                    </aui:select>

					<aui:input name="numberOfCategories" label="<%= numberOfCategoriesLabel %>">
                        <aui:validator name="digits"/>
                        <aui:validator name="min">1</aui:validator>
                        <aui:validator name="required">
                            function() {
                            return (<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %> == document.getElementById('<portlet:namespace/>createContentsType').value);
                            }
                        </aui:validator>

                    </aui:input>
					<aui:input name="baseCategoryName" label="<%= baseCategoryNameLabel %>">
                        <aui:validator name="required">
                            function() {
                            return (<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %> == document.getElementById('<portlet:namespace/>createContentsType').value);
                            }
                        </aui:validator>
                    </aui:input>
				</span>

                    <%
                        String numberOfVocabularyLabel = "Number of vocabulary";
                        String baseVocabularyNameLabel = "Base Vocabulary Name";
                    %>
                    <span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %>"
                          class="<portlet:namespace />contentsTypeGroup" style="display:none;">
					<aui:input name="numberOfVocabulary" label="<%= numberOfVocabularyLabel %>">
                        <aui:validator name="digits"/>
                        <aui:validator name="min">1</aui:validator>
                        <aui:validator name="required">
                            function() {
                            return (<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %> == document.getElementById('<portlet:namespace/>createContentsType').value);
                            }
                        </aui:validator>
                    </aui:input>
					<aui:input name="baseVocabularyName" label="<%= baseVocabularyNameLabel %>">
                        <aui:validator name="required">
                            function() {
                            return (<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %> == document.getElementById('<portlet:namespace/>createContentsType').value);
                            }
                        </aui:validator>
                    </aui:input>
				</span>

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

<aui:script use="aui-base">
    // Manage GroupID list display
    var createContentsType = document.getElementById('<portlet:namespace/>createContentsType');
    createContentsType.addEventListener("change load", function() {
        //--------------------------------
        // Contents Creation fields switch
        //--------------------------------
        var cmp_str = "<portlet:namespace/>contentsType" + createContentsType.value;
        var ctg = document.getElementsByClassName("<portlet:namespace />contentsTypeGroup");
        for (var i = 0; i < ctg.length; i++) {
            ctg[i].style.display = (cmp_str === document.getElementById(this).getAttribute("id")) ? "block" : "none";
        }
    });
</aui:script>

<%-- Thread List Update --%>
<portlet:resourceURL id="<%=LDFPortletKeys.CMD_CATEGORY_LIST %>" var="categoryListURL"/>

<script type="text/html" id="<portlet:namespace />category_options">
    <option value="<@= categoryId @>" selected="<@= selected @>"><@= categoryName @></option>
</script>

<script type="text/html" id="<portlet:namespace />vocabulary_options">
    <option value="<@= vocabularyId @>"><@= vocabularyName @></option>
</script>

<aui:script use="aui-base,liferay-form">
    // Update category list
    function <portlet:namespace/>categoryListUpdate() {
        var defer = $.Deferred();

        Liferay.Service(
            '/assetcategory/get-vocabulary-categories',
            {
                vocabularyId:document.getElementById('<portlet:namespace/>vocabularyId').value ,
                start: -1 ,
                end: -1,
                "+obc":"com.liferay.portlet.asset.util.comparator.AssetCategoryCreateDateComparator"
            },
            function(dataIn) {
                var data = dataIn;

                Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
                    (function() {
                        var _ = _lodash;

                        //Load Template
                        var tmpl = _.template(document.getElementById('<portlet:namespace/>category_options').innerHTML);
                        var listAll = tmpl({
                            categoryId:"<%= String.valueOf(AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID) %>",
                            categoryName:"(None)",
                            selected:"true"
                        });

                        _.map(data,function(n) {
                            listAll +=
                                tmpl(
                                    {
                                        categoryId:(n.categoryId) ? _.escape(n.categoryId) : "",
                                        categoryName:(n.titleCurrentValue) ? _.escape(n.titleCurrentValue) : "",
                                        selected:"false"
                                    }
                                );
                        });
                        var catObj = document.getElementById('<portlet:namespace/>parentCategoryId');
                        catObj.empty();
                        catObj.append(listAll);
                        defer.resolve();

                    })()
                }, function(error) {
                    console.error(error)
                });

            }
        );
        return defer.promise();
    }

    // Vocabulary ID
    document.getElementById('<portlet:namespace/>vocabularyId')
        .addEventListener("change load", function(event) {
            //Update thread list
            <portlet:namespace/>categoryListUpdate();
        })

    // Group (Site)
    document.getElementById('<portlet:namespace/>group')
        .addEventListener("change load", function(event) {
            //Update thread list
            <portlet:namespace/>vocabularyUpdate()
                .then(function() {
                    <portlet:namespace/>categoryListUpdate();
                });
        })

    function Deferred (){
        let res,rej,p = new Promise((a,b)=>(res = a, rej = b));
        p.resolve = res;
        p.reject = rej;
        return p;
    }

    // Update vocabulary list
    function <portlet:namespace/>vocabularyUpdate() {
        var defer = Deferred();
        document.getElementById('<portlet:namespace/>group')
            .addEventListener("change load", function(event) {
                Liferay.Service(
                    '/assetvocabulary/get-group-vocabularies',
                    {
                        groupId: document.getElementById('<portlet:namespace/>group').value
                    },
                    function(dataIn) {
                        var data = dataIn;

                        Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
                            (function() {
                                var _ = _lodash;

                                //Load Template
                                var tmpl = _.template(document.getElementById('<portlet:namespace/>vocabulary_options').innerHTML);
                                var listAll = "";
                                _.map(data,function(n) {
                                    listAll +=
                                        tmpl({
                                            vocabularyId:(n.vocabularyId) ? _.escape(n.vocabularyId) : "",
                                            vocabularyName:(n.titleCurrentValue) ? _.escape(n.titleCurrentValue) : ""
                                        });
                                });
                                var catObj = document.getElementById('<portlet:namespace/>vocabularyId');
                                catObj.empty();
                                catObj.append(listAll);
                                defer.resolve();

                            })()
                        }, function(error) {
                            console.error(error)
                        });
                    }
                );
            })

        return defer.resolve();
    }
</aui:script>
