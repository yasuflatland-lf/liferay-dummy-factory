package com.liferay.support.tools.service;

import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.service.JournalFolderLocalService;
import com.liferay.message.boards.service.MBCategoryLocalService;
import com.liferay.message.boards.service.MBThreadLocalService;
import com.liferay.portal.kernel.model.ShardedModel;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutSetPrototypeLocalService;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CompanyScopedIds.class)
public class CompanyScopedIds {

	public long companyId(long userId) {
		User user = _userLocalService.fetchUser(userId);

		if (user == null) {
			throw new IllegalArgumentException(
				"userId " + userId + " does not exist");
		}

		return user.getCompanyId();
	}

	public void ddmStructure(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_ddmStructureLocalService.fetchDDMStructure(id));
	}

	public void ddmTemplate(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_ddmTemplateLocalService.fetchDDMTemplate(id));
	}

	public void dlFolder(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id, _dlFolderLocalService.fetchDLFolder(id));
	}

	public void group(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id, _groupLocalService.fetchGroup(id));
	}

	public void journalFolder(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_journalFolderLocalService.fetchJournalFolder(id));
	}

	public void layoutSetPrototype(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_layoutSetPrototypeLocalService.fetchLayoutSetPrototype(id));
	}

	public void mbCategory(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_mbCategoryLocalService.fetchMBCategory(id));
	}

	public void organization(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_organizationLocalService.fetchOrganization(id));
	}

	public void role(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id, _roleLocalService.fetchRole(id));
	}

	public void thread(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id, _mbThreadLocalService.fetchMBThread(id));
	}

	public void user(long companyId, String parameter, long userId) {
		_requireCompany(
			companyId, parameter, userId, _userLocalService.fetchUser(userId));
	}

	public void userGroup(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_userGroupLocalService.fetchUserGroup(id));
	}

	public void vocabulary(long companyId, String parameter, long id) {
		_requireCompany(
			companyId, parameter, id,
			_assetVocabularyLocalService.fetchAssetVocabulary(id));
	}

	private static void _requireCompany(
		long companyId, String parameter, long id, ShardedModel entity) {

		if (entity == null) {
			throw new IllegalArgumentException(
				parameter + " " + id + " does not exist");
		}

		if (entity.getCompanyId() != companyId) {
			throw new IllegalArgumentException(
				parameter + " " + id +
					" does not belong to the caller's company");
		}
	}

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

	@Reference
	private DDMStructureLocalService _ddmStructureLocalService;

	@Reference
	private DDMTemplateLocalService _ddmTemplateLocalService;

	@Reference
	private DLFolderLocalService _dlFolderLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private JournalFolderLocalService _journalFolderLocalService;

	@Reference
	private LayoutSetPrototypeLocalService _layoutSetPrototypeLocalService;

	@Reference
	private MBCategoryLocalService _mbCategoryLocalService;

	@Reference
	private MBThreadLocalService _mbThreadLocalService;

	@Reference
	private OrganizationLocalService _organizationLocalService;

	@Reference
	private RoleLocalService _roleLocalService;

	@Reference
	private UserGroupLocalService _userGroupLocalService;

	@Reference
	private UserLocalService _userLocalService;

}
