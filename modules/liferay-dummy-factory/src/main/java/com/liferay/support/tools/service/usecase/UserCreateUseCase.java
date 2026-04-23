package com.liferay.support.tools.service.usecase;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.UserBatchSpec;
import com.liferay.support.tools.service.UserCreator;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UserCreateUseCase.class)
public class UserCreateUseCase {

	public UserCreateUseCase() {
	}

	UserCreateUseCase(
		LayoutSetLocalService layoutSetLocalService, UserCreator userCreator) {

		_layoutSetLocalService = layoutSetLocalService;
		_userCreator = userCreator;
	}

	public BatchResult<UserItemResult> create(
			long creatorUserId, long companyId, UserBatchSpec spec,
			ProgressCallback progress)
		throws Throwable {

		BatchResult<User> result = _userCreator.create(
			creatorUserId, companyId, spec, progress);

		boolean generateLayouts = spec.generatePersonalSiteLayouts();

		List<UserItemResult> items = new ArrayList<>(result.items().size());

		for (User user : result.items()) {
			items.add(_toItemResult(user, generateLayouts));
		}

		if (result.success()) {
			return BatchResult.success(result.requested(), items, result.skipped());
		}

		return BatchResult.failure(
			result.requested(), items, result.skipped(), result.error());
	}

	private UserItemResult _toItemResult(User user, boolean generateLayouts) {
		long groupId = 0L;
		String publicUuid = null;
		String privateUuid = null;

		if (generateLayouts) {
			Group personalSite = user.getGroup();

			if (personalSite != null) {
				groupId = personalSite.getGroupId();

				try {
					LayoutSet publicLayoutSet =
						_layoutSetLocalService.fetchLayoutSet(groupId, false);
					LayoutSet privateLayoutSet =
						_layoutSetLocalService.fetchLayoutSet(groupId, true);

					if (publicLayoutSet != null) {
						publicUuid = _nullIfEmpty(
							publicLayoutSet.getLayoutSetPrototypeUuid());
					}

					if (privateLayoutSet != null) {
						privateUuid = _nullIfEmpty(
							privateLayoutSet.getLayoutSetPrototypeUuid());
					}
				}
				// fetchLayoutSet failure is non-fatal: the user entity was already
				// committed. UUID data is supplementary; callers treat null as
				// "not linked". Error subclasses propagate normally — catch(Exception)
				// does not intercept them.
				catch (Exception e) {
					_log.error(
						"Failed to fetch layout sets for user " +
							user.getUserId() + " (groupId=" + groupId +
								"); returning without layout set UUIDs",
						e);
				}
			}
		}

		return new UserItemResult(
			user.getEmailAddress(), user.getScreenName(), user.getUserId(),
			groupId, publicUuid, privateUuid);
	}

	private static String _nullIfEmpty(String value) {
		return ((value == null) || value.isEmpty()) ? null : value;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserCreateUseCase.class);

	@Reference
	private LayoutSetLocalService _layoutSetLocalService;

	@Reference
	private UserCreator _userCreator;

}
