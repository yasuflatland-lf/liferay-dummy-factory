
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
<aui:nav-bar cssClass="collapse-basic-search" markupView="lexicon">
	<aui:nav cssClass="navbar-nav">
		<aui:nav-item label="Web Contents" selected="<%= true %>" />
	</aui:nav>
</aui:nav-bar>

<div class="container-fluid-1280">
	<liferay-ui:success key="success" message="Web contents created successfully" />
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.WCM %>" var="journalEditURL">
		<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_WCM %>" />
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
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
			
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			%>

			<aui:form action="<%= journalEditURL %>" method="post" name="fm" >
				<aui:input name="numberOfArticles" label="<%= numberOfArticlesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseTitle" label="<%= baseTitleLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="groupIds" label="<%= groupIdLabel %>" multiple="<%= true %>" >
					<aui:option label="<%= defaultOption %>" value="<%= themeDisplay.getScopeGroupId() %>" selected="<%= true %>" />
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
								<div id="<portlet:namespace />randomLink" style="display:none;">
									<label class="control-label"><%= linkListsLabel %>
										<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#<portlet:namespace />fakeGenInfo">
	                  						</a>
									</label>
						            <div class="collapsed collapse" id="<portlet:namespace />fakeGenInfo" aria-expanded="false" >
										<p>In terms of "Image links to insert into the generated contents" text area, you can add urls manually, but you can also generate them automatically. Please go to Configuration page of this portlet and generate image urls<p>
						            </div>											
									<aui:input label="" rows="5" name="linkLists" type="textarea" value="<%=linkList %>" placeholder="Input URLs each row">
								        <aui:validator name="required">
							                function() {
						                        return (0 < AUI.$('#<portlet:namespace />randomAmount').val);
							                }
								        </aui:validator>				
									</aui:input>
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
			
			<liferay-ui:upload-progress
				id="<%= progressId %>"
				message="creating..."
			/>	
						
		</aui:fieldset>
	</aui:fieldset-group>
</div>

<portlet:resourceURL id="/ldf/image/list" var="linkListURL" />

<aui:script use="aui-base, liferay-form">
	var processStart = A.one('#<portlet:namespace />processStart');
	
	processStart.on(
	    'click',
	    function() {
	    	event.preventDefault();
			<%= progressId %>.startProgress();
			submitForm(document.<portlet:namespace />fm);
	    }
	);
    
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