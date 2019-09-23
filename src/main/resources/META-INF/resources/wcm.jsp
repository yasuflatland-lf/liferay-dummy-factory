
<%@ include file="/init.jsp"%>
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
	inverted="<%= true %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Web Contents") %>'
/>

<div class="container-fluid-1280">


	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
		
			<liferay-ui:success key="success" message="Web contents created successfully" />
			<liferay-ui:error exception="<%= Exception.class %>" message="Error occured. Please see console log" />
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.WCM %>" var="journalEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_WCM %>" />
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>		
			
            <div class="entry-title form-group">
                <h1>Create Web Contents&nbsp;&nbsp;
                    <a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
                    </a>
                </h1>
            </div>
        
            <div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
                <blockquote class="blockquote-info">
                    <small>Example</small>
                    <p>if you enter the values <code>3</code> and <code>webContent</code> the portlet will create three web content articles: <code>webContent1</code>, <code>webContent2</code>, and <code>webContent3</code>.<p>
                </blockquote>
            
                <p>You must be signed in as an administrator in order to create web content articles</p>
                <p>The counter always starts at <code>1</code></p>
				<p>If no site is selected, the default site will be <code>liferay.com</code></p>
				<p>If no site is selected, the default site will be <code>liferay.com</code></p>
            </div>

			<%
			String numberOfArticlesLabel= "Enter the number of web content articles you would like to create";
			String baseTitleLabel= "Enter the base title";
			String baseArticleLabel = "Enter the contents";
			String defaultOption = "(None)";
			String groupIdLabel = "Select a site to assign the web content articles to";
			String localesLabel = "Select languages";
			String fakeContentsGenerateEnableLabel = "Generate Fake Contents";
			String linkListsLabel = "Image links to insert into the generated contents";
			String titleWordsLabel = "Amount of words for the title";
			String randomAmountLabel = "Amount of links in the generated contents";
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

			<aui:form action="<%= journalEditURL %>" method="post" name="fm"  onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
				<aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>
			
				<aui:input name="numberOfArticles" label="<%= numberOfArticlesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseTitle" label="<%= baseTitleLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="groupIds" label="<%= groupIdLabel %>" multiple="<%= true %>" >
					<aui:option label="<%= defaultOption %>" value="<%= guestGroupId %>" selected="<%= true %>" />
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

				<aui:select name="folderId" label="<%= folderIdLabel %>" >
					<aui:option label="<%= defaultOption %>" data-group-id="<%= guestGroupId %>" value="<%= JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID %>" selected="<%= true %>" />
				</aui:select>							
					
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
				
					<div class="row">
						<aui:fieldset cssClass="col-md-12">
							
							<%
							Set<Locale> locales = LanguageUtil.getAvailableLocales(themeDisplay.getSiteGroupId());
			
							%>
							<aui:select name="locales" label="<%= localesLabel %>" multiple="<%= true %>">
								<%
								for (Locale availableLocale : locales) {
								%>
									<aui:option label="<%= availableLocale.getDisplayName(locale) %>" value="<%= LocaleUtil.toLanguageId(availableLocale) %>"
									selected="<%= availableLocale.toString().equals(LocaleUtil.getDefault().toString()) %>" />
								<%
								}
								%>
							</aui:select>
							<aui:input type="toggle-switch" name="neverExpire" label="<%= neverExpireLabel %>" value="true" />
							<aui:input type="toggle-switch" name="neverReview" label="<%= neverReviewLabel %>" value="true" />
							
							
							<aui:select name="createContentsType" label="<%= createContentsTypeLabel %>" >
								<aui:option label="Simple Contents Create" value="<%= String.valueOf(LDFPortletKeys.WCM_SIMPLE_CONTENTS_CREATE) %>" />
								<aui:option label="Dummy Contents Create" value="<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %>" />
								<aui:option label="Structure Template Select Contents Create" value="<%= String.valueOf(LDFPortletKeys.WCM_STRUCTURE_TEMPLATE_SELECT_CREATE) %>" />
							</aui:select>
																
							<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.WCM_SIMPLE_CONTENTS_CREATE) %>" class="<portlet:namespace />contentsTypeGroup">
								<aui:input name="baseArticle" label="<%= baseArticleLabel %>" cssClass="lfr-textarea-container" type="textarea" wrap="soft" />
							</span>	
							<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %>" class="<portlet:namespace />contentsTypeGroup" style="display:none;">
								
								<aui:input name="titleWords" label="<%= titleWordsLabel %>" placeholder="10" >
									<aui:validator name="digits" />
									<aui:validator name="min">0</aui:validator>
							        <aui:validator name="required">
						                function() {
					                        return (<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
						                }
							        </aui:validator>											
								</aui:input>												
								<aui:input name="totalParagraphs" label="<%= totalParagraphsLabel %>" placeholder="10" >
									<aui:validator name="digits" />
									<aui:validator name="min">0</aui:validator>
							        <aui:validator name="required">
						                function() {
					                        return (<%= String.valueOf(LDFPortletKeys.WCM_DUMMY_CONTENTS_CREATE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
						                }
							        </aui:validator>											
								</aui:input>												
								<aui:input name="randomAmount" label="<%= randomAmountLabel %>" placeholder="4" >
									<aui:validator name="digits" />
									<aui:validator name="min">0</aui:validator>
								</aui:input>			
								<%
								String urlListLabel = "URL list where the crawler fetching images from (multiple URLs can be configured by comma separated, but takes longer to process.)";
								String linkListLabel = "Corrected image links / custom image links to save";
								%>
								
								<div id="<portlet:namespace />randomLink">
									<label class="control-label"><%= linkListsLabel %>
										<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#<portlet:namespace />fakeGenInfo">
	                  					</a>
									</label>
						            <div class="collapsed collapse" id="<portlet:namespace />fakeGenInfo" aria-expanded="false" >
										<p>In terms of "Image links to insert into the generated contents" text area, you can add urls manually, but you can also generate them automatically to click Fetch links button.<p>
						            </div>			
	           	                    <aui:input type="text" name="urlList" value="https://www.shutterstock.com/photos" label="<%=urlListLabel %>" />
									<aui:input rows="5" name="linkLists" type="textarea" value="" placeholder="Input URLs each row" label="<%=linkListLabel %>">
								        <aui:validator name="required">
							                function() {
						                        return (0 < AUI.$('#<portlet:namespace />randomAmount').val);
							                }
								        </aui:validator>				
									</aui:input>
									<aui:button name="fetchLinks" cssClass="btn btn-primary" value="Fetch links" />
									<span id="<portlet:namespace />linkLoader" class="loading-animation hide"></span>
								</div>									
							</span>		
								
							<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.WCM_STRUCTURE_TEMPLATE_SELECT_CREATE) %>" class="<portlet:namespace />contentsTypeGroup" style="display:none;">
								<%
									String ddmStructureLabel = "Journal Structures";
									String ddmTemplateLabel = "Journal Templates";
								%>									
								<aui:select name="ddmStructureId" label="<%= ddmStructureLabel %>" multiple="<%= true %>">
									<%
										boolean onlyDefault = (ddmStructures.size() == 1) ? true : false;
										for (DDMStructure ddmStructure : ddmStructures) {
									%>
									<aui:option label="<%= ddmStructure.getName(locale) %>" value="<%= ddmStructure.getPrimaryKey() %>"
									selected="<%=onlyDefault %>"/>
									<%
										}
									%>
								</aui:select>
								<aui:select name="ddmTemplateId" label="<%= ddmTemplateLabel %>" multiple="<%= true %>">
									<%
										for (DDMTemplate ddmTemplate : ddmTemplates) {
									%>
									<aui:option label="<%= ddmTemplate.getName(locale) %>" value="<%= ddmTemplate.getPrimaryKey() %>"/>
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
	</aui:fieldset-group>
</div>

<aui:script>
	function <portlet:namespace />execCommand() {
		<%= progressId %>.startProgress();
		submitForm(document.<portlet:namespace />fm);
	}
</aui:script>

<aui:script use="aui-base,liferay-form">
	var randomAmount = A.one('#<portlet:namespace />randomAmount');
	
	$('#<portlet:namespace />randomAmount').on(
	    'input load',
	    function() {
			$('#<portlet:namespace />randomLink').toggle((0 < randomAmount.val()));
	    }
	);
	    
	var createContentsType = A.one('#<portlet:namespace />createContentsType');
	
	$('#<portlet:namespace />createContentsType').on(
	    'change load',
	    function() {
	    	$('.<portlet:namespace />contentsTypeGroup').each(function(index){
	    		var cmp_str = "<portlet:namespace />contentsType" + createContentsType.val();
	    		$(this).toggle((cmp_str === $(this).attr("id")));
	    	});
	    }
	);
	
	
</aui:script>

<script type="text/html" id="<portlet:namespace />journal_folders">
    <option value="<@= folderId @>" data-group-id="<@= groupId @>" ><@= name @></option>
</script>

<aui:script use="aui-base,liferay-form">
	
	// Select Folder
	var groupIds = A.one('#<portlet:namespace />groupIds');
	
	$('#<portlet:namespace />groupIds').on(
	    'change load',
	    function() {
			Liferay.Service(
			  '/journal.journalfolder/get-folders',
			  {
			    groupId: groupIds.val()
			  },
			  function(obj) {
				//Load Template
				var tmpl = _.template($('#<portlet:namespace />journal_folders').html());
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
				var folderListObj = $('#<portlet:namespace />folderId')
				folderListObj.empty();
				folderListObj.append(listAll);
			  }
			);
	    }
	);	
	
</aui:script>

<portlet:resourceURL id="/ldf/image/list" var="linkListURL" />

<aui:script use="aui-base">
	var linkLoader = A.one('#<portlet:namespace />linkLoader');
    var fetchLinks = A.one('#<portlet:namespace />fetchLinks');
    var linkLists = $('#<portlet:namespace />linkLists');
    var urlList = A.one('#<portlet:namespace />urlList');

    fetchLinks.on(
        'click',
        function(event) {
            event.preventDefault();
            Liferay.Util.toggleDisabled('#<portlet:namespace />fetchLinks', true);
            linkLoader.show();
            var data = Liferay.Util.ns(
                '<portlet:namespace />',
                {
                    numberOfCrawlers: 15,
                    maxDepthOfCrawling: 3,
                    maxPagesToFetch: 100,
                    urls: urlList.val()
                }
            );

			$.ajax(
                '<%= linkListURL.toString() %>',
                {
                    data: data,
                    success: function(data) {
                    	var currentText = linkLists.val();
                    	linkLists.val(currentText + data.urlstr);
			            Liferay.Util.toggleDisabled('#<portlet:namespace />fetchLinks', false);
			            linkLoader.hide();
                    }
                }
            );

        }
    );

</aui:script>