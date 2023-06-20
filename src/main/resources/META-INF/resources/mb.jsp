<%@ include file="/init.jsp"%>

<clay:navigation-bar
	inverted="<%= false %>"
	navigationItems='<%= dummyFactoryDisplayContext.getNavigationBarItems("Message Board") %>'
/>

<div class="container-fluid container-fluid-max-xl container-view">

	<div class="sheet">
		<div class="panel-group panel-group-flush">
			<aui:fieldset>

				<liferay-ui:success key="success" message="Web contents created successfully" />
				<%@ include file="/command_select.jspf"%>

				<portlet:actionURL name="<%= LDFPortletKeys.MB %>" var="journalEditURL">
					<portlet:param name="<%= LDFPortletKeys.MODE %>" value="<%=LDFPortletKeys.MODE_MB %>" />
					<portlet:param name="redirect" value="<%=portletURL.toString()%>" />
				</portlet:actionURL>

				<div id="<portlet:namespace />Header0" role="tab">
					<div aria-controls="<portlet:namespace />Collapse0" aria-expanded="false"
						 class="collapse-icon collapse-icon-middle panel-toggler" data-toggle="liferay-collapse"
						 href="#<portlet:namespace />Collapse0" role="button">
						<h1>Create Message Board <small><liferay-ui:icon-help message="usage"/></small></h1>
					</div>
				</div>

				<div aria-expanded="false" aria-labelledby="<portlet:namespace />Header0"
					 class="collapse panel-collapse" id="<portlet:namespace />Collapse0" role="tabpanel">
					<div class="alert alert-info">
						<h4>Example</h4>
						<p>if you enter the values <code>3</code> and <code>thread</code> the portlet will create three threads: <code>thread1</code>, <code>thread2</code>, and <code>thread3</code>.<p>
						<hr class="separator" />
						<ul>
							<li>You must be signed in as an administrator in order to create message board threads / categories</li>
							<li>The counter always starts at <code>1</code></li>
							<li>If no site is selected, the default site will be <code>liferay.com</code></li>
						</ul>
					</div>
				</div>

				<%
					String numberOfMBLabel= "Enter the number of threads / categories you would like to create";
					String defaultOption = "(None)";
					String groupIdLabel = "Select a site to assign the threads / categories to";
					String createContentsTypeLabel = "Select create type";
					String subjectLabel = "Subject";
					String bodyLabel = "Body";
					String anonymousLabel = "Anonymous";
					String allowPingbacksLabel = "Allow ping back";
					String priorityLabel = "Priority";
					String categoryNameLabel = "Category Name";
					String descriptionLabel = "Description";
					String threadListLabel = "Thread list";
					String categoryIdLabel = "Categories";
					String formatLabel = "format";

					List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
					final String groupName = GroupConstants.GUEST;
					final long companyId = PortalUtil.getDefaultCompanyId();
					final long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
				%>

				<aui:form action="<%= journalEditURL %>" method="post" name="fm"
						  onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "execCommand();" %>'>
					<aui:input name="<%= LDFPortletKeys.COMMON_PROGRESS_ID %>" value="<%= progressId %>" type="hidden"/>

					<aui:input name="numberOfMB" label="<%= numberOfMBLabel %>" >
						<aui:validator name="digits" />
						<aui:validator name="min">1</aui:validator>
						<aui:validator name="required" />
					</aui:input>

					<aui:select name="createContentsType" label="<%= createContentsTypeLabel %>" >
						<aui:option label="Threads" value="<%= String.valueOf(LDFPortletKeys.MB_THREAD_CREATE) %>" />
						<aui:option label="Categories" value="<%= String.valueOf(LDFPortletKeys.MB_CATEGORY_CREATE) %>" />
						<aui:option label="Reply" value="<%= String.valueOf(LDFPortletKeys.MB_REPLY_CREATE) %>" />
					</aui:select>

					<span id="<portlet:namespace />groupIdsWrap">
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
				</span>
					<span id="<portlet:namespace />siteGroupIdWrap" style="display:none;">
					<aui:select name="siteGroupId" label="<%= groupIdLabel %>" >
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
				</span>

					<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.MB_REPLY_CREATE) %>" class="<portlet:namespace />contentsTypeGroup" style="display:none;">
					<aui:select name="threadId" label="<%=threadListLabel %>" >
					</aui:select>
				</span>

					<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.MB_THREAD_CREATE) %>" class="<portlet:namespace />contentsTypeGroup">
					<div class="row">
						<aui:fieldset cssClass="col-md-6">
							<span id="<portlet:namespace />categoryIdWrap" class="<portlet:namespace />contentsTypeGroup">
								<aui:select name="categoryId" label="<%= categoryIdLabel %>" >
								</aui:select>
							</span>
							<aui:input name="subject" label="<%= subjectLabel %>" cssClass="lfr-textarea-container" >
								<aui:validator name="required">
									function() {
										return <%= String.valueOf(LDFPortletKeys.MB_THREAD_CREATE) %> == document.getElementById('<portlet:namespace />createContentsType').value
									}
								</aui:validator>
							</aui:input>
							<aui:input name="body" label="<%= bodyLabel %>" rows="5" type="textarea" cssClass="lfr-textarea-container" >
								<aui:validator name="required">
									function() {
										return <%= String.valueOf(LDFPortletKeys.MB_THREAD_CREATE) %> == document.getElementById('<portlet:namespace />createContentsType').value
									}
								</aui:validator>
							</aui:input>
						</aui:fieldset>
						<aui:fieldset cssClass="col-md-6">
							<aui:input name="priority" label="<%= priorityLabel %>" >
								<aui:validator name="number" />
								<aui:validator name="min">0</aui:validator>
							</aui:input>
							<aui:select name="format" label="<%= formatLabel %>" >
								<aui:option label="bbcode" value="<%= String.valueOf(LDFPortletKeys.MB_FORMAT_BBCODE) %>" />
								<aui:option label="HTML" value="<%= String.valueOf(LDFPortletKeys.MB_FORMAT_BBCODE) %>" />
							</aui:select>

							<aui:input name="anonymous" type="toggle-switch" label="<%= anonymousLabel %>" value="<%= false %>"/>
							<aui:input name="allowPingbacks" type="toggle-switch" label="<%= allowPingbacksLabel %>" value="<%= false %>"/>
						</aui:fieldset>
					</div>
				</span>
					<span id="<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.MB_CATEGORY_CREATE) %>" class="<portlet:namespace />contentsTypeGroup" style="display:none;">
					<aui:input name="categoryName" label="<%= categoryNameLabel %>" cssClass="lfr-textarea-container" >
						<aui:validator name="required">
							function() {
							return (<%= String.valueOf(LDFPortletKeys.MB_CATEGORY_CREATE) %> == document.getElementById('<portlet:namespace />createContentsType').value);
							}
						</aui:validator>
					</aui:input>
					<aui:input name="description" label="<%= descriptionLabel %>" rows="3" type="textarea" cssClass="lfr-textarea-container" >
						<aui:validator name="required">
							function() {
							return (<%= String.valueOf(LDFPortletKeys.MB_CATEGORY_CREATE) %> == document.getElementById('<portlet:namespace />createContentsType').value);
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

<portlet:resourceURL id="/ldf/image/list" var="linkListURL" />

<aui:script>
	function <portlet:namespace />execCommand() {
		<%= progressId %>.startProgress();
		submitForm(document.<portlet:namespace />fm);
	}
</aui:script>


<%-- Thread List Update --%>
<portlet:resourceURL id="<%=LDFPortletKeys.CMD_MB_LIST %>" var="mbListURL" >
	<portlet:param name="<%=Constants.CMD %>" value="<%=MBMVCResourceCommand.CMD_THREAD_LIST%>" />
<%--	<portlet:param name="siteGroupId" value="" />--%>
</portlet:resourceURL>

<script type="text/html" id="<portlet:namespace />message_options">
    <option value="<@= threadId @>" data-root-massage-id="<@= rootMessageId @>"><@= rootMessageSubject @></option>
</script>

<script type="text/html" id="<portlet:namespace />category_options">
    <option value="<@= categoryId @>" ><@= categoryName @></option>
</script>

<aui:script use="aui-base,aui-io-request,liferay-form">

	var handleClick = function() {
		const createContentsType = document.getElementById('<portlet:namespace />createContentsType');
	
		//--------------------------------
		// Contents Creation fields switch
		//--------------------------------
		var cmp_str = "<portlet:namespace />contentsType" + createContentsType.value;

		var ctg = document.getElementsByClassName("<portlet:namespace />contentsTypeGroup");
		for (var i = 0; i < ctg.length; i++) {
			ctg[i].style.display = (cmp_str === ctg[i].getAttribute("id")) ? "block" : "none";
		}

		//--------------------------------
		// At reply create, remove multiple for select site.
		//--------------------------------
		if(<%= String.valueOf(LDFPortletKeys.MB_REPLY_CREATE) %> == createContentsType.value) {
			document.getElementById("<portlet:namespace />siteGroupIdWrap").style.display = "block";
			document.getElementById("<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.MB_REPLY_CREATE) %>").style.display = "block";
			document.getElementById("<portlet:namespace />categoryIdWrap").style.display = "none";
			document.getElementById("<portlet:namespace />groupIdsWrap").style.display = "none";

			//Update thread list
			<portlet:namespace />threadListUpdate();

		} else if(<%= String.valueOf(LDFPortletKeys.MB_CATEGORY_CREATE) %> == createContentsType.value) {
			document.getElementById("<portlet:namespace />siteGroupIdWrap").style.display = "block";
			document.getElementById("<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.MB_CATEGORY_CREATE) %>").style.display = "block";
			document.getElementById("<portlet:namespace />categoryIdWrap").style.display = "none";
			document.getElementById("<portlet:namespace />groupIdsWrap").style.display = "none";
		} else {
			document.getElementById("<portlet:namespace />siteGroupIdWrap").style.display = "none";
			document.getElementById("<portlet:namespace />contentsType<%= String.valueOf(LDFPortletKeys.MB_THREAD_CREATE) %>").style.display = "block";
			document.getElementById("<portlet:namespace />categoryIdWrap").style.display = "block";
			document.getElementById("<portlet:namespace />groupIdsWrap").style.display = "block";
		}

	}
	// Manage GroupID list display
	document.getElementById('<portlet:namespace />createContentsType')
	.addEventListener('change', handleClick);
	document.getElementById('<portlet:namespace />createContentsType')
	.addEventListener('load', handleClick);

	function ajax(cmd, path, data, handler) {
		var xmlhttp = new XMLHttpRequest();

		xmlhttp.onreadystatechange = function() {
			if (xmlhttp.readyState == XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
				if (xmlhttp.status == 200) {
					var jsonData = JSON.parse(xmlhttp.response);
					handler(jsonData);
				}
				else {
					console.error('status: ' + xmlhttp.status);
				}
			}
		};

		xmlhttp.open(cmd, path, true);
		xmlhttp.send(data);
	}

	// Update thread list
	function <portlet:namespace />threadListUpdate() {
		var data = Liferay.Util.ns(
			'<portlet:namespace />',
			{
				<%=Constants.CMD %>: '<%=MBMVCResourceCommand.CMD_THREAD_LIST%>',
				siteGroupId: document.getElementById('<portlet:namespace />siteGroupId').value
			}
		);

		var threadListUpdateHandlerFunc = function(dataIn) {
			var data = dataIn;

			Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
				(function() {
					var _ = _lodash;

					//Load Template
					var tmpl = _.template(document.getElementById('<portlet:namespace />message_options').innerHTML);
					var listAll = "";
					_.map(data,function(n) {
						listAll +=
						tmpl(
						  {
							rootMessageSubject:(n.rootMessageSubject) ? _.escape(n.rootMessageSubject) : "",
							threadId:(n.threadId) ? _.escape(n.threadId) : "",
							rootMessageId:(n.rootMessageId) ? _.escape(n.rootMessageId) : ""
						  }
						);
					});
					
					Liferay.Loader.require("<%=jqueryResolver %>", function(_jq) {
						(function() {
							var $ = _jq;
							console.log(listAll);
							const pageObj = $('#<portlet:namespace />threadId');
							pageObj.empty();
							pageObj.append(listAll);
						})()
					}, function(error) {
						console.error(error)
					});

				})()
			}, function(error) {
				console.error(error)
			});

		}
		Liferay.Util.fetch(
			'<%= mbListURL.toString() %>',
			{
				body: data,
				method: 'POST',
			}
		)
		.then((response) => {
			return response.json();
		})
		.then((data) => {
			threadListUpdateHandlerFunc(data);
		});
		// ajax("POST", '<%= mbListURL.toString() %>', data, threadListUpdateHandlerFunc )
	}

	function <portlet:namespace />categoryListUpdate() {
		var data = Liferay.Util.ns(
			'<portlet:namespace />',
			{
				<%=Constants.CMD %>: '<%=MBMVCResourceCommand.CMD_CATEGORY_LIST%>',
				groupIds: document.getElementById('<portlet:namespace />groupIds').value.join(',')
			}
		);

	var categoryListUpdateHandlerFunc = function(dataIn) {
			var data = dataIn;

			Liferay.Loader.require("<%=lodashResolver %>", function(_lodash) {
				(function() {
					var _ = _lodash;

					//Load Template
					var tmpl = _.template(document.getElementsByClassName('<portlet:namespace />category_options').innerHTML);
					var listAll = tmpl({
						categoryId:"<%= String.valueOf(MBCategoryConstants.DEFAULT_PARENT_CATEGORY_ID) %>",
						categoryName:"Default"
					});
					_.map(data,function(n) {
						listAll +=
						tmpl(
						  {
							categoryId:(n.categoryId) ? _.escape(n.categoryId) : "",
							categoryName:(n.categoryName) ? _.escape(n.categoryName) : ""
						  }
						);
					});
					var pageObj = document.getElementsByClassName('<portlet:namespace />categoryId');
					pageObj.empty();
					pageObj.append(listAll);

				})()
			}, function(error) {
				console.error(error)
			});
		}

		ajax("POST", '<%= mbListURL.toString() %>', data, categoryListUpdateHandlerFunc )
	}

	document.createElement("<portlet:namespace/>siteGroupId")
	.addEventListener('change', function(event) {
		//Update thread list
		<portlet:namespace />threadListUpdate();
	});

	document.createElement("<portlet:namespace/>groupIds")
	.addEventListener('change', function(event) {
		var groupIds = document.createElement("<portlet:namespace/>groupIds").value;
		if(groupIds.length == 1) {
			//Update category list
			document.getElementById("<portlet:namespace />categoryIdWrap").style.display = "block";
			<portlet:namespace />categoryListUpdate();
		} else {
			document.getElementById("<portlet:namespace />categoryIdWrap").style.display = "none";
		}
	});


</aui:script>

