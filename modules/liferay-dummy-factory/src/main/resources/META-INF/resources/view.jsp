<%@ include file="/init.jsp" %>

<portlet:resourceURL id="/ldf/org" var="orgResourceURL" />
<portlet:resourceURL id="/ldf/page" var="pageResourceURL" />
<portlet:resourceURL id="/ldf/role" var="roleResourceURL" />
<portlet:resourceURL id="/ldf/site" var="siteResourceURL" />
<portlet:resourceURL id="/ldf/user" var="userResourceURL" />
<portlet:resourceURL id="/ldf/wcm" var="wcmResourceURL" />
<portlet:resourceURL id="/ldf/doc" var="docResourceURL" />
<portlet:resourceURL id="/ldf/doc/upload" var="docUploadResourceURL" />
<portlet:resourceURL id="/ldf/vocabulary" var="vocabularyURL" />
<portlet:resourceURL id="/ldf/blog" var="blogResourceURL" />
<portlet:resourceURL id="/ldf/category" var="categoryURL" />
<portlet:resourceURL id="/ldf/mb-category" var="mbCategoryURL" />
<portlet:resourceURL id="/ldf/mb-thread" var="mbThreadURL" />
<portlet:resourceURL id="/ldf/mb-reply" var="mbReplyURL" />
<portlet:resourceURL id="/ldf/company" var="companyResourceURL" />
<portlet:resourceURL id="/ldf/data" var="dataResourceURL" />
<portlet:resourceURL id="/ldf/progress" var="progressResourceURL" />

<%
ResourceBundle resourceBundle = portletConfig.getResourceBundle(locale);
JSONObject languageKeys = JSONFactoryUtil.createJSONObject();
Enumeration<String> enumeration = resourceBundle.getKeys();

while (enumeration.hasMoreElements()) {
	String key = enumeration.nextElement();

	languageKeys.put(key, resourceBundle.getString(key));
}
%>

<script>
	Liferay.Language._cache = Liferay.Language._cache || {};
	Object.assign(Liferay.Language._cache, <%= languageKeys.toJSONString() %>);
</script>

<react:component
	module="{App} from liferay-dummy-factory"
	props='<%=
		HashMapBuilder.<String, Object>put(
			"actionResourceURLs", HashMapBuilder.<String, Object>put(
				"/ldf/org", orgResourceURL
			).put(
				"/ldf/page", pageResourceURL
			).put(
				"/ldf/role", roleResourceURL
			).put(
				"/ldf/site", siteResourceURL
			).put(
				"/ldf/user", userResourceURL
			).put(
				"/ldf/wcm", wcmResourceURL
			).put(
				"/ldf/doc", docResourceURL
			).put(
				"/ldf/doc/upload", docUploadResourceURL
			).put(
				"/ldf/vocabulary", vocabularyURL
			).put(
				"/ldf/blog", blogResourceURL
			).put(
				"/ldf/category", categoryURL
			).put(
				"/ldf/mb-category", mbCategoryURL
			).put(
				"/ldf/mb-thread", mbThreadURL
			).put(
				"/ldf/mb-reply", mbReplyURL
			).put(
				"/ldf/company", companyResourceURL
			).build()
		).put(
			"dataResourceURL", dataResourceURL
		).put(
			"namespace", renderResponse.getNamespace()
		).put(
			"progressResourceURL", progressResourceURL
		).build()
	%>'
/>
