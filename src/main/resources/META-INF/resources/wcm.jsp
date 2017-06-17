<%@ include file="/init.jsp"%>

<div class="container-fluid-1280">
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.WCM %>" var="journalEditURL">
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
            
                <ul>
					<li>You must be signed in as an administrator in order to create web content articles<li>
                    <li>The counter always starts at <code>1</code></li>
					<li>If no site is selected, the default site will be <code>liferay.com</code><li>
                </ul>
            
                <h3>Creating Large Batches of Web Content Articles</h3>
                <ul>
                    <li>If the number of web content articles is large (over <code>100</code>), go to <i>Control Panel -> Server Administration -> Log Levels -> Add Category</i>, and add <code>com.liferay.support.tools</code> and set to <code>INFO</code> to track progress (batches of 10%)</li>
                    <li>It may take some time (even for the logs to show) to create a large number of web content articles, and the page will hang until the process is complete; you can query the database if you are uncertain of the progress</li>
                </ul>
            </div>

			<%
			String numberOfArticlesLabel= "Enter the number of web content articles you would like to create";
			String baseTitleLabel= "Enter the base title";
			String baseArticleLabel = "Enter the contents";
			String defaultOption = "(None)";
			String groupLabel = "Select a site to assign the web content articles to";
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			%>

			<aui:form action="<%= journalEditURL %>" method="post" >
				<aui:input name="numberOfArticles" label="<%= numberOfArticlesLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseTitle" label="<%= baseTitleLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseArticle" label="<%= baseArticleLabel %>" cssClass="lfr-textarea-container" type="textarea" wrap="soft">
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="groups" label="<%= groupLabel %>"  >
					<aui:option label="<%= defaultOption %>" value="<%= themeDisplay.getScopeGroupId() %>" />
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

				<aui:button-row>
					<aui:button type="submit" value="Run" cssClass="btn-lg btn-block btn-primary"/>
				</aui:button-row>	
			</aui:form>	
		</aui:fieldset>
	</aui:fieldset-group>
</div>
