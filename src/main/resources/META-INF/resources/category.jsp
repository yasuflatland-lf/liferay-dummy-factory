<%@ include file="/init.jsp"%>

<clay:navigation-bar
	inverted="<%= true %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Category / Vocabulary") %>'
/>

<div class="container-fluid-1280">


	<aui:fieldset-group markupView="lexicon">
		<aui:fieldset>
		
			<liferay-ui:success key="success" message="Category / Vocabraly created successfully" />
			<%@ include file="/command_select.jspf"%>
		
			<portlet:actionURL name="<%= LDFPortletKeys.CATEGORY %>" var="categoryEditURL">
				<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_CATEGORY %>" />
				<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
			</portlet:actionURL>	
				
			<div class="entry-title form-group">
				<h1>Create Category / Vocabulary&nbsp;&nbsp;
					<a aria-expanded="false" class="collapse-icon collapsed icon-question-sign" data-toggle="collapse" href="#navPillsCollapse0">
					</a>
				</h1>
			</div>

			<div class="collapsed collapse" id="navPillsCollapse0" aria-expanded="false" >
				<blockquote class="blockquote-info">
				    <small>Example</small>
					<p>if you enter the values <code>3</code> in the "category / vocabulary", the portlet will create three categories: <code>category1</code>, <code>category2</code>, and <code>category3</code>.<p>
				</blockquote>

				<ul>
					<li>You must be signed in as an administrator in order to create categories / vocabularies</li>
					<li>The counter always starts at <code>1</code></li>
					<li>If no site is selected, the default site will be <code>liferay.com</code></li>
				</ul>

			</div>

			<%
			String numberOfCategoriesLabel = "Enter the number of categories / vocabralies you would like to create";
			String baseCategoryNameLabel= "Enter the base Category / Vocabulary name";
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			final String groupName = GroupConstants.GUEST;
			final long companyId = PortalUtil.getDefaultCompanyId();
			final long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();

			String defaultOption = "(None)";

			String scopeGroupdId = String.valueOf(themeDisplay.getScopeGroupId());
			String groupLabel = "Select a site to assign the pages to";
			String pageLabel = "Select a parent page";
			String createContentsTypeLabel = "Select create type";
			String parentCategoryIdLabel = "Enter the parent category ID";
			String privateLayoutLabel = "Make pages private";
			String hiddenLabel = "Hide from Navigation Menu";
			String vocabularyIdLabel = "Vocabulary ID";
			%>

			<aui:form action="<%= categoryEditURL %>" method="post" name="fm"  onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
				<aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>
				
				<aui:select name="createContentsType" label="<%= createContentsTypeLabel %>" >
					<aui:option label="Category" value="<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %>" />
					<aui:option label="Vocabulary" value="<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %>" />
				</aui:select>

				<aui:select name="group" label="<%= groupLabel %>" >
					<aui:option label="<%= defaultOption %>" value="<%= guestGroupId %>" />
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

				<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %>" class="<portlet:namespace />contentsTypeGroup" >
					<%
					List<AssetVocabulary> assetVocabularies = AssetVocabularyServiceUtil.getGroupVocabularies(
							guestGroupId, true);					

					%>
					
					<aui:select name="vocabularyId" label="<%= vocabularyIdLabel %>" >
						<%
						for (AssetVocabulary assetVocabulary : assetVocabularies) {
						%>
							<aui:option label="<%= assetVocabulary.getName() %>" value="<%= assetVocabulary.getVocabularyId() %>"/>
						<%
						}
						%>
					</aui:select>

					<aui:select name="parentCategoryId" label="<%=parentCategoryIdLabel %>" >
						<aui:option value="<%= String.valueOf(AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID) %>" selected="true">(None)</aui:option>
					</aui:select>
					
					<aui:input name="numberOfCategories" label="<%= numberOfCategoriesLabel %>" >
						<aui:validator name="digits" />
						<aui:validator name="min">1</aui:validator>
				        <aui:validator name="required">
			                function() {
		                        return (<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
			                }
				        </aui:validator>											

					</aui:input>
					<aui:input name="baseCategoryName" label="<%= baseCategoryNameLabel %>" >
				        <aui:validator name="required">
			                function() {
		                        return (<%= String.valueOf(LDFPortletKeys.C_CATEGORY_CREATE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
			                }
				        </aui:validator>											
					</aui:input>
				</span>

				<%
				String numberOfVocabularyLabel = "Number of vocabulary";
				String baseVocabularyNameLabel = "Base Vocabulary Name";
				%>
				<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %>" class="<portlet:namespace />contentsTypeGroup" style="display:none;">
					<aui:input name="numberOfVocabulary" label="<%= numberOfVocabularyLabel %>" >
						<aui:validator name="digits" />
						<aui:validator name="min">1</aui:validator>
				        <aui:validator name="required">
			                function() {
		                        return (<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
			                }
				        </aui:validator>											
					</aui:input>
					<aui:input name="baseVocabularyName" label="<%= baseVocabularyNameLabel %>" >
				        <aui:validator name="required">
			                function() {
		                        return (<%= String.valueOf(LDFPortletKeys.C_VOCABULARY_CREATE) %> == AUI.$('#<portlet:namespace />createContentsType').val());
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
	</aui:fieldset-group>
</div>

<aui:script>
	function <portlet:namespace />execCommand() {
		<%= progressId %>.startProgress();
		submitForm(document.<portlet:namespace />fm);
	}
</aui:script>

<aui:script use="aui-base">	
    // Manage GroupID list display
    var createContentsType = A.one('#<portlet:namespace />createContentsType');
	$('#<portlet:namespace />createContentsType').on(
	    'change load',
	    function() {
	    	//--------------------------------
	    	// Contents Creation fields switch
	    	//--------------------------------
    		var cmp_str = "<portlet:namespace />contentsType" + createContentsType.val();

	    	$('.<portlet:namespace />contentsTypeGroup').each(function(index){
				$(this).toggle((cmp_str === $(this).attr("id")));
	    	});
	    }
	);   	
</aui:script>

<%-- Thread List Update --%>
<portlet:resourceURL id="<%=LDFPortletKeys.CMD_CATEGORY_LIST %>" var="categoryListURL" />

<script type="text/html" id="<portlet:namespace />category_options">
    <option value="<@= categoryId @>" selected="<@= selected @>"><@= categoryName @></option>
</script>

<script type="text/html" id="<portlet:namespace />vocabulary_options">
    <option value="<@= vocabularyId @>" ><@= vocabularyName @></option>
</script>

<aui:script use="aui-base,liferay-form">
	
	// Update category list
	function <portlet:namespace />categoryListUpdate() {
		var defer = $.Deferred();
		
		Liferay.Service(
		  '/assetcategory/get-vocabulary-categories',
		  {
		    vocabularyId:$('#<portlet:namespace />vocabularyId').val() ,
		    start: -1 ,
		    end: -1,
		    "+obc":"com.liferay.portlet.asset.util.comparator.AssetCategoryCreateDateComparator" 
		  },
		  function(dataIn) {
		  	var data = dataIn;
		  	
		    Liferay.Loader.require("<%=bootstrapRequire %>", function(_lodash) {
		        (function() {
		            var _ = _lodash;
		            
					//Load Template
					var tmpl = _.template($('#<portlet:namespace />category_options').html());
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
					var catObj = $('#<portlet:namespace />parentCategoryId');
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
	$('#<portlet:namespace />vocabularyId').on(
		'change load',
		function(event) {
			//Update thread list
			<portlet:namespace />categoryListUpdate();
		}
	);
	
	// Group (Site)
	$('#<portlet:namespace />group').on(
		'change load',
		function(event) {
		
			//Update thread list
			<portlet:namespace />vocabularyUpdate()
			.then(function() {
				<portlet:namespace />categoryListUpdate();
			});
		}
	);	
	
	// Update vocabulary list
	function <portlet:namespace />vocabularyUpdate() {
		var defer = $.Deferred();
		
		$('#<portlet:namespace />group').on(
			'change load',
			function(event) {
				Liferay.Service(
				  '/assetvocabulary/get-group-vocabularies',
				  {
				    groupId: $('#<portlet:namespace />group').val()
				  },
				  function(dataIn) {
				    var data = dataIn;
				    
				    Liferay.Loader.require("<%=bootstrapRequire %>", function(_lodash) {
				        (function() {
				            var _ = _lodash;
				            
							//Load Template
							var tmpl = _.template($('#<portlet:namespace />vocabulary_options').html());
							var listAll = "";
							_.map(data,function(n) {
								listAll += 
								tmpl({
									vocabularyId:(n.vocabularyId) ? _.escape(n.vocabularyId) : "",
									vocabularyName:(n.titleCurrentValue) ? _.escape(n.titleCurrentValue) : ""
								});
							});
							var catObj = $('#<portlet:namespace />vocabularyId');
							catObj.empty();
							catObj.append(listAll);	
							defer.resolve();		    
				            
				        })()
				    }, function(error) {
				        console.error(error)
				    });					  
				  }
				);	
			}
		);	
		return defer.promise();	
	}	
</aui:script>