package com.liferay.support.tools.service.usecase;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.SiteCreator;
import com.liferay.support.tools.service.SiteMembershipType;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SiteCreateUseCase.class)
public class SiteCreateUseCase {

	public SiteCreateUseCase() {
	}

	SiteCreateUseCase(
		LayoutSetLocalService layoutSetLocalService, SiteCreator siteCreator) {

		_layoutSetLocalService = layoutSetLocalService;
		_siteCreator = siteCreator;
	}

	public BatchResult<SiteItemResult> create(
			long userId, long companyId, BatchSpec batchSpec,
			SiteMembershipType membershipType, long parentGroupId,
			long siteTemplateId, boolean manualMembership,
			boolean inheritContent, boolean active, String description,
			long publicLayoutSetPrototypeId, long privateLayoutSetPrototypeId,
			ProgressCallback progress)
		throws Throwable {

		BatchResult<Group> result = _siteCreator.create(
			userId, companyId, batchSpec, membershipType, parentGroupId,
			siteTemplateId, manualMembership, inheritContent, active,
			description, publicLayoutSetPrototypeId,
			privateLayoutSetPrototypeId, progress);

		List<SiteItemResult> items = new ArrayList<>(result.items().size());

		for (Group group : result.items()) {
			items.add(_toItemResult(group));
		}

		if (result.success()) {
			return BatchResult.success(result.requested(), items, result.skipped());
		}

		return BatchResult.failure(
			result.requested(), items, result.skipped(), result.error());
	}

	private SiteItemResult _toItemResult(Group group) {
		long groupId = group.getGroupId();

		String publicUuid = null;
		String privateUuid = null;

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
		// fetchLayoutSet failure is non-fatal: the site group was already
		// committed. UUID data is supplementary; callers treat null as
		// "not linked". Error subclasses propagate normally — catch(Exception)
		// does not intercept them.
		catch (Exception e) {
			_log.error(
				"Failed to fetch layout sets for group " + groupId +
					"; returning without layout set UUIDs",
				e);
		}

		return new SiteItemResult(
			groupId,
			group.getName(LocaleUtil.getDefault()),
			group.isInheritContent(),
			group.getParentGroupId(),
			publicUuid,
			privateUuid);
	}

	private static String _nullIfEmpty(String value) {
		return ((value == null) || value.isEmpty()) ? null : value;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteCreateUseCase.class);

	@Reference
	private LayoutSetLocalService _layoutSetLocalService;

	@Reference
	private SiteCreator _siteCreator;

}
