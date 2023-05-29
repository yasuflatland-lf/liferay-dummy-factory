<%@ include file="/init.jsp" %>
<%
    List<DDMTemplate> ddmTemplates =
            DDMTemplateLocalServiceUtil.getTemplates(
                    themeDisplay.getScopeGroupId(),
                    PortalUtil.getClassNameId(com.liferay.dynamic.data.mapping.model.DDMStructure.class.getName())
            );
    List<DDMStructure> ddmStructures =
            JournalFolderServiceUtil.getDDMStructures(
                    PortalUtil.getCurrentAndAncestorSiteGroupIds(themeDisplay.getScopeGroupId()),
                    JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID,
                    JournalFolderConstants.RESTRICTION_TYPE_INHERIT
            );
%>
<clay:navigation-bar
        inverted="<%= false %>"
        navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Web Contents") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">
    <div class="sheet">
        <div class="panel-group panel-group-flush">
            <aui:fieldset>

                <liferay-ui:success key="success" message="Web contents created successfully"/>
                <liferay-ui:error exception="<%= Exception.class %>" message="Error occured. Please see console log"/>
                <%@ include file="/command_select.jspf" %>

                <portlet:actionURL name="<%= LDFPortletKeys.WCM %>" var="journalEditURL">
                    <portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_WCM %>"/>
                    <portlet:param name="redirect" value="<%=portletURL.toString()%>"/>
                </portlet:actionURL>

                <div id="<portlet:namespace />Header0" role="tab">
                    <div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
                         class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
                         href="#<portlet:namespace />Collapse0" role="button">
                        <h1>Create Web Contents <liferay-ui:icon-help message="usage"/></h1>
                    </div>
                </div>

                <div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
                     class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
                    <blockquote class="blockquote-info">
                        <small>Example</small>
                        <p>if you enter the values <code>3</code> and <code>webContent</code> the portlet will create three
                            web content articles: <code>webContent1</code>, <code>webContent2</code>, and
                            <code>webContent3</code>.
                        <p>
                    </blockquote>

                    <p>You must be signed in as an administrator in order to create web content articles</p>
                    <p>The counter always starts at <code>1</code></p>
                    <p>If no site is selected, the default site will be <code>liferay.com</code></p>
                    <p>If no site is selected, the default site will be <code>liferay.com</code></p>
                </div>

                <%
                    String numberOfArticlesLabel = "Enter the number of web content articles you would like to create";
                    String baseTitleLabel = "Enter the base title";
                    String baseArticleLabel = "Enter the contents";
                    String defaultOption = "(None)";
                    String groupIdLabel = "Select a site to assign the web content articles to";
                    String localesLabel = "Select languages";
                    String titleWordsLabel = "Amount of words for the title";
                    String totalParagraphsLabel = "Paragraphes count";
                    String createContentsTypeLabel = "Select create contents type";
                    String folderIdLabel = "Journal Folder ID of the target folder";
                    String neverExpireLabel = "Never Expired";
                    String neverReviewLabel = "Never Review";

                    List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
                    final String groupName = GroupConstants.GUEST;
                    final long companyId = PortalUtil.getDefaultCompanyId();
                    final long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
                %>

                <aui:form action="<%= journalEditURL %>" method="post" name="fm"
                          onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
                    <aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>

                    <aui:input name="numberOfArticles" label="<%= numberOfArticlesLabel %>">
                        <aui:validator name="digits"/>
                        <aui:validator name="min">1</aui:validator>
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:input name="baseTitle" label="<%= baseTitleLabel %>" cssClass="lfr-textarea-container">
                        <aui:validator name="required"/>
                    </aui:input>
                    <aui:select name="groupIds" label="<%= groupIdLabel %>" multiple="<%= true %>">
                        <aui:option label="<%= defaultOption %>" value="<%= guestGroupId %>" selected="<%= true %>"/>
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

                    <aui:select name="folderId" label="<%= folderIdLabel %>">
                        <aui:option label="<%= defaultOption %>" data-group-id="<%= guestGroupId %>"
                                    value="<%= JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID %>" selected="<%= true %>"/>
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

                                <%
                                    Set<Locale> locales = LanguageUtil.getAvailableLocales(themeDisplay.getSiteGroupId());

                                %>
                                <aui:select name="locales" label="<%= localesLabel %>" multiple="<%= true %>">
                                    <%
                                        for (Locale availableLocale : locales) {
                                    %>
                                    <aui:option label="<%= availableLocale.getDisplayName(locale) %>"
                                                value="<%= LocaleUtil.toLanguageId(availableLocale) %>"
                                                selected="<%= availableLocale.toString().equals(LocaleUtil.getDefault().toString()) %>"/>
                                    <%
                                        }
                                    %>
                                </aui:select>
                                <aui:input type="toggle-switch" name="neverExpire" label="<%= neverExpireLabel %>"
                                           value="true"/>
                                <aui:input type="toggle-switch" name="neverReview" label="<%= neverReviewLabel %>"
                                           value="true"/>


                                <aui:select name="createContentsType" label="<%= createContentsTypeLabel %>">
                                    <aui:option label="Simple Contents Create"
                                                value="<%= String.valueOf(LDFPortletKeys.WCM_SIMPLE_CONTENTS_CREATE) %>"/>
                                    <aui:option label="Dummy Contents Create"
                                                value="<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %>"/>
                                    <aui:option label="Structure Template Select Contents Create"
                                                value="<%= String.valueOf(LDFPortletKeys.WCM_STRUCTURE_TEMPLATE_SELECT_CREATE) %>"/>
                                </aui:select>

                                <span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.WCM_SIMPLE_CONTENTS_CREATE) %>"
                                      class="<portlet:namespace />contentsTypeGroup">
								<aui:input name="baseArticle" label="<%= baseArticleLabel %>"
                                           cssClass="lfr-textarea-container" type="textarea" wrap="soft"/>
							</span>
                                <span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %>"
                                      class="<portlet:namespace />contentsTypeGroup" style="display:none;">

								<%
                                    String urlListLabel = "URL list where the crawler fetching images from (multiple URLs can be configured by comma separated, but takes longer to process.)";
                                    String linkListLabel = "Corrected image links / custom image links to save";
                                    String randomAmountLabel = "Amount of links in the generated contents";
                                %>

								<div class="row">
									<aui:fieldset cssClass="col-md-6">

                                        <aui:input name="titleWords" label="<%= titleWordsLabel %>" placeholder="10">
                                            <aui:validator name="digits"/>
                                            <aui:validator name="min">0</aui:validator>
                                            <aui:validator name="required">
                                                function() {
                                                return (<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %> == document.getElementById('<portlet:namespace/>createContentsType').value);
                                                }
                                            </aui:validator>
                                        </aui:input>
                                        <aui:input name="totalParagraphs" label="<%= totalParagraphsLabel %>"
                                                   placeholder="10">
                                            <aui:validator name="digits"/>
                                            <aui:validator name="min">0</aui:validator>
                                            <aui:validator name="required">
                                                function() {
                                                return (<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %> == document.getElementById('<portlet:namespace/>createContentsType').value);
                                                }
                                            </aui:validator>
                                        </aui:input>
                                        <label class="control-label"><%= randomAmountLabel %>
											<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign"
                                               data-toggle="collapse" href="#<portlet:namespace />imageGenInfo"></a>
										</label>
                                        <div class="collapsed collapse" id="<portlet:namespace />imageGenInfo"
                                             aria-expanded="false">
											<p>The image links for this functionality are refereed from <code><%=linkListLabel %></code> text area on the right. You can add urls manually and also generate them automatically to click <code>Fetch links</code> button.<p>
							            </div>

                                        <aui:input name="randomAmount" label="" placeholder="0" value="0">
                                            <aui:validator name="digits"/>
                                            <aui:validator name="min">0</aui:validator>
                                        </aui:input>


                                    </aui:fieldset>
									<aui:fieldset cssClass="col-md-6">

										<div id="<portlet:namespace />randomLink">
											<label class="control-label">URL&nbsp;List
												<a aria-expanded="false"
                                                   class="collapse-icon collapsed icon-question-sign"
                                                   data-toggle="collapse" href="#<portlet:namespace />urlListInfo"></a>
											</label>
								            <div class="collapsed collapse" id="<portlet:namespace />urlListInfo"
                                                 aria-expanded="false">
												<p><%=urlListLabel %></p>
								            </div>
			           	                    <aui:input type="text" name="urlList"
                                                       value="https://imgur.com/search?q=flower" label=""/>
											<aui:button-row>
                                                <aui:button name="fetchLinks" cssClass="btn btn-primary"
                                                            value="Fetch links"/>
                                            </aui:button-row>

											<aui:input rows="5" name="linkLists" type="textarea" value=""
                                                       placeholder="Input URLs each row" label="<%=linkListLabel %>">
                                                <aui:validator name="required">
                                                    function() {
                                                    return (0 < document.getElementById('<portlet:namespace/>randomAmount').value);
                                                    }
                                                </aui:validator>
                                            </aui:input>
											<span id="<portlet:namespace />linkLoader"
                                                  class="loading-animation hide"></span>
										</div>

                                    </aui:fieldset>
								</div>

							</span>

                                <span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.WCM_STRUCTURE_TEMPLATE_SELECT_CREATE) %>"
                                      class="<portlet:namespace />contentsTypeGroup" style="display:none;">
								<%
                                    String ddmStructureLabel = "Journal Structures";
                                    String ddmTemplateLabel = "Journal Templates";
                                %>
								<aui:select name="ddmStructureId" label="<%= ddmStructureLabel %>"
                                            multiple="<%= true %>">
                                    <%
                                        boolean onlyDefault = (ddmStructures.size() == 1) ? true : false;
                                        for (DDMStructure ddmStructure : ddmStructures) {
                                    %>
                                    <aui:option label="<%= ddmStructure.getName(locale) %>"
                                                value="<%= ddmStructure.getPrimaryKey() %>"
                                                selected="<%=onlyDefault %>"/>
                                    <%
                                        }
                                    %>
                                </aui:select>
								<aui:select name="ddmTemplateId" label="<%= ddmTemplateLabel %>" multiple="<%= true %>">
                                    <%
                                        for (DDMTemplate ddmTemplate : ddmTemplates) {
                                    %>
                                    <aui:option label="<%= ddmTemplate.getName(locale) %>"
                                                value="<%= ddmTemplate.getPrimaryKey() %>"/>
                                    <%
                                        }
                                    %>
                                </aui:select>
							</span>
                            </aui:fieldset>
                        </div>

                        <div class="row">
                            <aui:fieldset cssClass="col-md-12">
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

<aui:script use="aui-base">
    document.addEventListener('DOMContentLoaded', function () {
        var randomAmount = document.getElementById('<portlet:namespace/>randomAmount');

        // Initialize
        document.getElementById('<portlet:namespace/>randomLink').style.display = "none";

        document.createElement("<portlet:namespace/>randomAmount")
            .addEventListener("input load visibility", function(event) {
                document.getElementById('<portlet:namespace/>randomLink').classList.toggle((0 < randomAmount.value));
            });

        var createContentsType = document.getElementById('<portlet:namespace/>createContentsType');

        document.createElement("<portlet:namespace/>createContentsType")
            .addEventListener("change load", function(event) {
                var ctg = document.getElementsByClassName("<portlet:namespace />contentsTypeGroup");
                for (var i = 0; i < ctg.length; i++) {
                    var cmp_str = "<portlet:namespace/>contentsType" + createContentsType.value;
                    ctg[i].style.display = (cmp_str === document.getElementById(this).getAttribute("id")) ? "block" : "none";
                }
            });
    });
</aui:script>

<script type="text/html" id="<portlet:namespace />journal_folders">
    <option value="<@= folderId @>" data-group-id="<@= groupId @>"><@= name @></option>
</script>

<aui:script use="aui-base">
    // Select Folder
    var groupIds = document.getElementById('<portlet:namespace/>groupIds');
    groupIds.addEventListener("change load", function() {

        Liferay.Service(
            '/journal.journalfolder/get-folders',
            {
                groupId: groupIds.value[0]
            },
            function(objIn) {
                var obj = objIn;

                Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
                    //(function() {
                    var _ = _lodash;

                    //Load Template
                    var tmpl = _.template(document.getElementById('<portlet:namespace/>journal_folders').innerHTML);
                    var listAll = tmpl({
                        name:"(None)",
                        folderId:0,
                        groupId:<%=themeDisplay.getScopeGroupId() %>
                    });
                    _.map(obj,function(data) {
                        listAll +=
                            tmpl(
                                {
                                    name:data.name,
                                    folderId:data.folderId,
                                    groupId:data.groupId
                                }
                            );
                    });
                    var folderListObj = document.getElementById('#<portlet:namespace/>folderId')
                    folderListObj.empty();
                    folderListObj.append(listAll);
                }, function(error) {
                    console.error(error)
                });
            }
        );
    });
</aui:script>

<portlet:resourceURL id="/ldf/image/list" var="linkListURL"/>

<aui:script use="aui-base">
    var linkLoader = document.getElementById('<portlet:namespace/>linkLoader');
    var fetchLinks = document.getElementById('<portlet:namespace/>fetchLinks');
    var linkLists = document.getElementById('<portlet:namespace/>linkLists');
    var urlList = document.getElementById('<portlet:namespace/>urlList');
    var randomAmount = document.getElementById('<portlet:namespace/>randomAmount')

    fetchLinks.addEventListener("click", function(event) {
        event.preventDefault();
        Liferay.Util.toggleDisabled('#<portlet:namespace/>fetchLinks', true);
        linkLoader.show();
        var data = Liferay.Util.ns(
            '<portlet:namespace/>',
            {
                numberOfCrawlers: 15,
                maxDepthOfCrawling: 3,
                maxPagesToFetch: 100,
                randomAmount: randomAmount.value,
                urls: urlList.value
            }
        );

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
        ajax("POST", '<%= linkListURL.toString() %>', data, function(data) {
            var currentText = linkLists.value;
            linkLists.value = currentText + data.urlstr;
            Liferay.Util.toggleDisabled('#<portlet:namespace/>fetchLinks', false);
            linkLoader.hide();
        });

    })
</aui:script>