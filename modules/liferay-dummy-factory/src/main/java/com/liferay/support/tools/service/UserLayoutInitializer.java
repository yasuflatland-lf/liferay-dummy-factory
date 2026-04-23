package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UserLayoutInitializer.class)
public class UserLayoutInitializer {

	public void init(User user) throws PortalException {
		Group group = user.getGroup();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(user.getCompanyId());
		serviceContext.setUserId(user.getUserId());

		if (_layoutLocalService.getLayoutsCount(group, false) == 0) {
			_layoutLocalService.addLayout(
				StringPool.BLANK, user.getUserId(), group.getGroupId(), false,
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, "Welcome",
				StringPool.BLANK, StringPool.BLANK,
				LayoutConstants.TYPE_PORTLET, false, "/welcome",
				serviceContext);
		}

		if (_layoutLocalService.getLayoutsCount(group, true) == 0) {
			_layoutLocalService.addLayout(
				StringPool.BLANK, user.getUserId(), group.getGroupId(), true,
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, "Welcome",
				StringPool.BLANK, StringPool.BLANK,
				LayoutConstants.TYPE_PORTLET, false, "/welcome",
				serviceContext);
		}
	}

	@Reference
	private LayoutLocalService _layoutLocalService;

}
