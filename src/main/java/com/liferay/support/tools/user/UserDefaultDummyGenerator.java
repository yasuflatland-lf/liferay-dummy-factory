
package com.liferay.support.tools.user;

import com.liferay.portal.kernel.exception.GroupFriendlyURLException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.sites.kernel.util.SitesUtil;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * User Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = UserDefaultDummyGenerator.class)
public class UserDefaultDummyGenerator extends DummyGenerator<UserContext> {

	@Override
	protected UserContext getContext(ActionRequest request)
		throws Exception {

		return new UserContext(request);
	}

	@Override
	protected void exec(ActionRequest request, UserContext paramContext)
		throws Exception {

		// Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		System.out.println(
			"Starting to create " + paramContext.getNumberOfusers() + " users");

		for (long i = 1; i <= paramContext.getNumberOfusers(); i++) {
			// Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfusers());

			StringBundler screenName = new StringBundler(2);
			screenName.append(paramContext.getBaseScreenName());

			// Add number more then one user
			if (1 < paramContext.getNumberOfusers()) {
				screenName.append(i);
			}

			StringBundler emailAddress = new StringBundler(2);
			emailAddress.append(screenName);
			emailAddress.append("@").append(paramContext.getBaseDomain());

			try {
				// Create user and apply roles
				User user = _userDataService.createUserData(
					paramContext.getServiceContext(),
					paramContext.getOrganizationIds(),
					paramContext.getGroupIds(), paramContext.getRoleIds(),
					paramContext.getUserGroupIds(), paramContext.isMale(),
					paramContext.isFakerEnable(), paramContext.getPassword(),
					screenName.toString(), emailAddress.toString(),
					paramContext.getBaseScreenName(), i,
					paramContext.getLocale());

				if (paramContext.isAutoUserPreLogin()) {
					// Generate private / public user page
					_userLayoutUtil.updateUserLayouts(user);
				}
				
				//Update Announcements Deliveries
				_userDataService.updateAnnouncementsDeliveries(
					user.getUserId(), paramContext.getAnnouncementsDeliveries());
				
				// My Profile and My Dashboard Template
				SitesUtil.updateLayoutSetPrototypesLinks(
					user.getGroup(), 
					paramContext.getPublicLayoutSetPrototypeId(),
					paramContext.getPrivateLayoutSetPrototypeId(),
					paramContext.isPublicLayoutSetPrototypeLinkEnabled(),
					paramContext.isPrivateLayoutSetPrototypeLinkEnabled());

			}
			catch (Exception e) {

				// Finish progress
				progressManager.finish();

				if (e instanceof GroupFriendlyURLException) {
					_log.error("group-friendly-url-error");
				}

				throw e;
			}
		}

		// Finish progress
		progressManager.finish();

	}

	@Reference
	private UserDataService _userDataService;

	@Reference
	private UserLayoutUtil _userLayoutUtil;

	private static final Log _log =
		LogFactoryUtil.getLog(UserDefaultDummyGenerator.class);
}
