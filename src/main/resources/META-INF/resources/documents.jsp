<%@ include file="/init.jsp"%>

<aui:nav-bar cssClass="collapse-basic-search" markupView="lexicon">
	<aui:nav cssClass="navbar-nav">
		<aui:nav-item label="Documents" selected="<%= true %>" />
	</aui:nav>
</aui:nav-bar>

<div class="container-fluid-1280">
	<liferay-ui:success key="success" message="Documents created successfully" />
	<%@ include file="/command_select.jspf"%>

	<portlet:actionURL name="<%= LDFPortletKeys.DOCUMENTS %>" var="documentEditURL">
		<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_DOCUMENTS %>" />
		<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
	</portlet:actionURL>

	<aui:fieldset-group markupView="lexicon">	
		<aui:fieldset>
            <div class="entry-title form-group">
                <h1>Create Documents&nbsp;&nbsp;
                    <a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
                    </a>
                </h1>
            </div>
        
            <div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
                <blockquote class="blockquote-info">
                    <small>Example</small>
                    <p>if you enter the values <code>3</code> and <code>doc</code> the portlet will create three documents: <code>doc1</code>, <code>doc2</code>, and <code>doc3</code>.<p>
                </blockquote>
            
                <ul>
					<li>You must be signed in as an administrator in order to create documents<li>
                    <li>The counter always starts at <code>1</code></li>
					<li>If no site is selected, the default site will be <code>liferay.com</code><li>
                </ul>
            
            </div>

			<%
			String numberOfDocumentsLabel= "Enter the number of documents you would like to create";
			String baseDocumentTitleLabel= "Enter the base document title (i.e. doc, newDoc, testDoc)";
			String baseDocumentDescriptionLabel = "Enter the base document description";
			String defaultOption = "(None)";
			String groupIdLabel = "Select a site to assign the documents to";
			String dlFolderIdLabel = "Select a folder where the document is created";
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			final String groupName = GroupConstants.GUEST;
			final long companyId = PortalUtil.getDefaultCompanyId();
			final long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();			
			%>

			<aui:form action="<%= documentEditURL %>" method="post" name="fm" >
				<aui:input name="numberOfDocuments" label="<%= numberOfDocumentsLabel %>" >
					<aui:validator name="digits" />
					<aui:validator name="min">1</aui:validator>
					<aui:validator name="required" />				
				</aui:input>
				<aui:input name="baseDocumentTitle" label="<%= baseDocumentTitleLabel %>" cssClass="lfr-textarea-container" >
					<aui:validator name="required" />				
				</aui:input>
				<aui:select name="groupId" label="<%= groupIdLabel %>"  >
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
				<aui:select name="folderId" label="<%= dlFolderIdLabel %>" >
					<aui:option label="<%= defaultOption %>" value="<%= String.valueOf(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) %>"/>
				</aui:select>						
				<aui:a href="#inputOptions" cssClass="collapse-icon collapsed icon-angle-down" title="Option" aria-expanded="false" data-toggle="collapse" >&nbsp;&nbsp;option</aui:a>
				<div class="collapsed collapse" id="inputOptions" aria-expanded="false" >
					<div class="row">
						<aui:fieldset cssClass="col-md-12">
						
							<aui:input name="baseDocumentDescription" label="<%= baseDocumentDescriptionLabel %>" cssClass="lfr-textarea-container" type="textarea" wrap="soft" />
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

<script type="text/html" id="<portlet:namespace />journal_folder_options">
    <option value="<@= folderId @>" ><@= name @></option>
</script>

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
	
	$('#<portlet:namespace />groupId').on(
		'change load',
		function(event) {
		
			Liferay.Service(
			  '/dlfolder/get-folders',
			  {
			    groupId: $('#<portlet:namespace />groupId').val(),
			    parentFolderId: "<%= String.valueOf(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) %>",
			    start: -1,
			    end: -1,
			    "+obc":"com.liferay.document.library.kernel.util.comparator.FolderIdComparator" 
			  },
			  function(data) {
                //Load Template
                var tmpl = _.template($('#<portlet:namespace />journal_folder_options').html());
                var listAll = tmpl({
                    folderId:"<%= String.valueOf(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) %>",
                    name:"(None)",
                    selected:"true"
                });
                
                _.map(data,function(n) {
                    listAll += 
                    tmpl(
                      {
                        folderId:(n.folderId) ? _.escape(n.folderId) : "",
                        name:(n.name) ? _.escape(n.name) : "",
                        selected:"false"
                      }
                    );
                });
                var catObj = $('#<portlet:namespace />folderId');
                catObj.empty();
                catObj.append(listAll);             			    
			  }
			);		
		}
	);		
</aui:script>
