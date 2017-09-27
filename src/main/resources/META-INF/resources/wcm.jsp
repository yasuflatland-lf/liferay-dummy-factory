<%@ include file="/init.jsp"%>

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
                    <p>if you enter the values <code>3</code> and <code>webContent</code> the portlet will create three blank sites: <code>webContent1</code>, <code>webContent2</code>, and <code>webContent3</code>.<p>
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
							<ul class="nav nav-tabs nav-justified" role="tablist">
					            <li class="active" role="presentation"><a aria-controls="fields" href="#<portlet:namespace />common" data-toggle="tab" role="tab" aria-expanded="true">Common</a></li>
					            <li role="presentation" class=""><a aria-controls="settings" href="#<portlet:namespace />detailed_contents" data-toggle="tab" role="tab" aria-expanded="false">Detailed Contents</a></li>
					        </ul>
					
					        <div class="tab-content">
					            <div role="tabpanel" class="tab-pane fade active in" id="<portlet:namespace />common">

									<div class="row">
										<aui:fieldset cssClass="col-md-6">
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
										</aui:fieldset>
										<aui:fieldset cssClass="col-md-6">
											<aui:input name="fakeContentsGenerateEnable" type="toggle-switch" label="<%= fakeContentsGenerateEnableLabel %>" value="<%= false %>"/>
											<span id="<portlet:namespace />randomContents" class="hide">
												
												<aui:input name="titleWords" label="<%= titleWordsLabel %>" placeholder="10" >
													<aui:validator name="digits" />
													<aui:validator name="min">0</aui:validator>
												</aui:input>												
												<aui:input name="totalParagraphs" label="<%= totalParagraphsLabel %>" placeholder="10" >
													<aui:validator name="digits" />
													<aui:validator name="min">0</aui:validator>
												</aui:input>												
												<aui:input name="randomAmount" label="<%= randomAmountLabel %>" placeholder="4" >
													<aui:validator name="digits" />
													<aui:validator name="min">0</aui:validator>
												</aui:input>												
												<label class="control-label"><%= linkListsLabel %>
													<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#<portlet:namespace />fakeGenInfo">
		                    						</a>
												</label>
									            <div class="collapsed collapse" id="<portlet:namespace />fakeGenInfo" aria-expanded="false" >
													<p>In terms of "Image links to insert into the generated contents" text area, you can add urls manually, but you can also generate them automatically. Please go to Configuration page of this portlet and generate image urls<p>
									            </div>											
												<aui:input label="" rows="5" name="linkLists" type="textarea" value="<%=linkList %>" placeholder="Input URLs each row"/>
											</span>		
											<span id="<portlet:namespace />manualContents">
												<aui:input name="baseArticle" label="<%= baseArticleLabel %>" cssClass="lfr-textarea-container" type="textarea" wrap="soft" />
											</span>		
										</aui:fieldset>
										
									</div>
					            </div><%-- common --%>
					            <div role="tabpanel" class="tab-pane fade" id="<portlet:namespace />detailed_contents">
									<div class="row">
										<aui:fieldset cssClass="col-md-12">
										</aui:fieldset>
									</div>					            
					            </div>
					        </div>				

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

<aui:script use="aui-base">
	var processStart = A.one('#<portlet:namespace />processStart');
	
	processStart.on(
	    'click',
	    function() {
	    	event.preventDefault();
			<%= progressId %>.startProgress();
			submitForm(document.<portlet:namespace />fm);
	    }
	);
    
	var fakeContentsGenerateEnable = A.one('#<portlet:namespace />fakeContentsGenerateEnable');
	
	fakeContentsGenerateEnable.on(
	    'click',
	    function() {
	    	console.log(fakeContentsGenerateEnable.val());
	    	$('#<portlet:namespace />randomContents').toggleClass('hide');
	    	$('#<portlet:namespace />manualContents').toggleClass('hide');
	    }
	);
	
</aui:script>