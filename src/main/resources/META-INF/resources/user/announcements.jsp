<%@ page import="com.liferay.announcements.kernel.model.AnnouncementsDelivery" %>
<%@ page import="com.liferay.announcements.kernel.model.AnnouncementsEntryConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.liferay.announcements.kernel.service.AnnouncementsDeliveryLocalServiceUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.counter.kernel.service.CounterLocalServiceUtil" %>

<%
List<AnnouncementsDelivery> deliveries = new ArrayList<AnnouncementsDelivery>(AnnouncementsEntryConstants.TYPES.length);

for (String type : AnnouncementsEntryConstants.TYPES) {
	long deliveryId = CounterLocalServiceUtil.increment();
	AnnouncementsDelivery delivery =
			AnnouncementsDeliveryLocalServiceUtil.createAnnouncementsDelivery(deliveryId);

	delivery.setType(type);
	delivery.setWebsite(true);

	deliveries.add(delivery);
}
%>

<liferay-ui:search-container>
	<liferay-ui:search-container-results
		results="<%= deliveries %>"
	/>

	<liferay-ui:search-container-row
		className="com.liferay.announcements.kernel.model.AnnouncementsDelivery"
		escapedModel="<%= true %>"
		keyProperty="deliveryId"
		modelVar="delivery"
	>
		<liferay-ui:search-container-column-text
			name="type"
			value="<%= LanguageUtil.get(request, delivery.getType()) %>"
		/>

		<liferay-ui:search-container-column-jsp
			name="email"
			path="/user/announcements_checkbox.jsp"
		/>

		<liferay-ui:search-container-column-jsp
			name="sms"
			path="/user/announcements_checkbox.jsp"
		/>

		<liferay-ui:search-container-column-jsp
			name="website"
			path="/user/announcements_checkbox.jsp"
		/>
		
	</liferay-ui:search-container-row>

	<liferay-ui:search-iterator
		markupView="lexicon"
	/>
</liferay-ui:search-container>