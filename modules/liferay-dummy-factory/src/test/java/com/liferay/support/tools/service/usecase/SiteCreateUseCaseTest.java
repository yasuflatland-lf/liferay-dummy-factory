package com.liferay.support.tools.service.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.SiteCreator;
import com.liferay.support.tools.service.SiteMembershipType;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SiteCreateUseCaseTest {

	@Test
	void createPopulatesUuidsWhenLayoutSetsPresent() throws Throwable {
		Group mockGroup = _group(201L, "Test Site", false, 0L);
		LayoutSet mockPublicLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class,
			Map.of("getLayoutSetPrototypeUuid", "pub-uuid-1"));
		LayoutSet mockPrivateLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class,
			Map.of("getLayoutSetPrototypeUuid", "priv-uuid-1"));

		SiteCreateUseCase useCase = new SiteCreateUseCase(
			_layoutSetLocalService(mockPublicLayoutSet, mockPrivateLayoutSet),
			_stubSiteCreator(List.of(mockGroup), 1));

		BatchResult<SiteItemResult> result = _callCreate(useCase, 1);

		assertTrue(result.success());
		SiteItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertEquals("Test Site", item.name());
		assertEquals("pub-uuid-1", item.publicLayoutSetPrototypeUuid());
		assertEquals("priv-uuid-1", item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createReturnsNullUuidsWhenLayoutSetsAbsent() throws Throwable {
		Group mockGroup = _group(201L, "Test Site", false, 0L);

		SiteCreateUseCase useCase = new SiteCreateUseCase(
			_layoutSetLocalService(null, null),
			_stubSiteCreator(List.of(mockGroup), 1));

		BatchResult<SiteItemResult> result = _callCreate(useCase, 1);

		assertTrue(result.success());
		SiteItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createNormalizesEmptyStringUuidToNull() throws Throwable {
		Group mockGroup = _group(201L, "Test Site", false, 0L);
		LayoutSet mockPublicLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class, Map.of("getLayoutSetPrototypeUuid", ""));
		LayoutSet mockPrivateLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class, Map.of("getLayoutSetPrototypeUuid", ""));

		SiteCreateUseCase useCase = new SiteCreateUseCase(
			_layoutSetLocalService(mockPublicLayoutSet, mockPrivateLayoutSet),
			_stubSiteCreator(List.of(mockGroup), 1));

		BatchResult<SiteItemResult> result = _callCreate(useCase, 1);

		assertTrue(result.success());
		SiteItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createMapsInheritContentAndParentGroupId() throws Throwable {
		Group mockGroup = _group(301L, "Child Site", true, 100L);

		SiteCreateUseCase useCase = new SiteCreateUseCase(
			_layoutSetLocalService(null, null),
			_stubSiteCreator(List.of(mockGroup), 1));

		BatchResult<SiteItemResult> result = _callCreate(useCase, 1);

		assertTrue(result.success());
		SiteItemResult item = result.items().get(0);
		assertEquals(301L, item.groupId());
		assertEquals("Child Site", item.name());
		assertTrue(item.inheritContent());
		assertEquals(100L, item.parentGroupId());
	}

	@Test
	void createHandlesLayoutSetFetchException() throws Throwable {
		Group mockGroup = _group(201L, "Test Site", false, 0L);

		LayoutSetLocalService throwingService =
			(LayoutSetLocalService)Proxy.newProxyInstance(
				LayoutSetLocalService.class.getClassLoader(),
				new Class<?>[] {LayoutSetLocalService.class},
				(proxy, method, args) -> {
					if ("fetchLayoutSet".equals(method.getName())) {
						throw new RuntimeException("DB connection error");
					}

					return null;
				});

		SiteCreateUseCase useCase = new SiteCreateUseCase(
			throwingService, _stubSiteCreator(List.of(mockGroup), 1));

		BatchResult<SiteItemResult> result = _callCreate(useCase, 1);

		assertTrue(result.success());
		SiteItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createPropagatesFailureWhenCreatorFails() throws Throwable {
		Group mockGroup = _group(201L, "Test Site", false, 0L);

		SiteCreateUseCase useCase = new SiteCreateUseCase(
			_layoutSetLocalService(null, null),
			_failingSiteCreator(List.of(mockGroup), 2, "Only 1 of 2 sites created"));

		BatchResult<SiteItemResult> result = _callCreate(useCase, 2);

		assertFalse(result.success());
		assertEquals(2, result.requested());
		assertEquals(1, result.skipped());
		assertEquals("Only 1 of 2 sites created", result.error());
		assertEquals(1, result.items().size());
	}

	private static Group _group(
		long groupId, String name, boolean inheritContent, long parentGroupId) {

		Map<String, Object> values = new java.util.HashMap<>();

		values.put("getGroupId", groupId);
		values.put("getName", name);
		values.put("isInheritContent", inheritContent);
		values.put("getParentGroupId", parentGroupId);

		return TestModelProxyUtil.proxy(Group.class, values);
	}

	private static LayoutSetLocalService _layoutSetLocalService(
		LayoutSet publicLayoutSet, LayoutSet privateLayoutSet) {

		return (LayoutSetLocalService)Proxy.newProxyInstance(
			LayoutSetLocalService.class.getClassLoader(),
			new Class<?>[] {LayoutSetLocalService.class},
			(proxy, method, args) -> {
				if ("fetchLayoutSet".equals(method.getName())) {
					boolean isPrivate = (Boolean)args[1];

					return isPrivate ? privateLayoutSet : publicLayoutSet;
				}

				return null;
			});
	}

	private static BatchResult<SiteItemResult> _callCreate(
			SiteCreateUseCase useCase, int count)
		throws Throwable {

		return useCase.create(
			1L, 10L,
			new BatchSpec(count, "testsite"),
			SiteMembershipType.OPEN,
			0L, 0L, true, false, true, "",
			0L, 0L,
			ProgressCallback.NOOP);
	}

	private static SiteCreator _stubSiteCreator(
		List<Group> groups, int requested) {

		return new SiteCreator() {

			@Override
			public BatchResult<Group> create(
				long userId, long companyId, BatchSpec batchSpec,
				SiteMembershipType membershipType, long parentGroupId,
				long siteTemplateId, boolean manualMembership,
				boolean inheritContent, boolean active, String description,
				long publicLayoutSetPrototypeId,
				long privateLayoutSetPrototypeId, ProgressCallback progress) {

				return BatchResult.success(requested, groups, 0);
			}

		};
	}

	private static SiteCreator _failingSiteCreator(
		List<Group> groups, int requested, String error) {

		return new SiteCreator() {

			@Override
			public BatchResult<Group> create(
				long userId, long companyId, BatchSpec batchSpec,
				SiteMembershipType membershipType, long parentGroupId,
				long siteTemplateId, boolean manualMembership,
				boolean inheritContent, boolean active, String description,
				long publicLayoutSetPrototypeId,
				long privateLayoutSetPrototypeId, ProgressCallback progress) {

				return BatchResult.failure(
					requested, groups, requested - groups.size(), error);
			}

		};
	}

}
