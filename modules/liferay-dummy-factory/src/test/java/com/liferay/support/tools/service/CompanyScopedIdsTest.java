package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompanyScopedIdsTest {

	@Test
	public void matchingCompanyIsAccepted() {
		Group group = TestModelProxyUtil.proxy(Group.class, Map.of("getCompanyId", 10L));

		assertDoesNotThrow(() -> validator(group).group(10L, "groupId", 20L));
	}

	@Test
	public void anotherCompanyIsRejected() {
		Group group = TestModelProxyUtil.proxy(Group.class, Map.of("getCompanyId", 11L));

		IllegalArgumentException error = assertThrows(
			IllegalArgumentException.class,
			() -> validator(group).group(10L, "groupId", 20L));

		assertEquals("groupId 20 does not belong to the caller's company", error.getMessage());
	}

	@Test
	public void missingEntityIsRejected() {
		IllegalArgumentException error = assertThrows(
			IllegalArgumentException.class,
			() -> validator(null).group(10L, "groupId", 20L));

		assertEquals("groupId 20 does not exist", error.getMessage());
	}

	@Test
	public void everyEntityResolverChecksCompanyAndExistence() throws Exception {
		Map<String, String> serviceFields = Map.ofEntries(
			Map.entry("ddmStructure", "_ddmStructureLocalService"),
			Map.entry("ddmTemplate", "_ddmTemplateLocalService"),
			Map.entry("dlFolder", "_dlFolderLocalService"),
			Map.entry("group", "_groupLocalService"),
			Map.entry("journalFolder", "_journalFolderLocalService"),
			Map.entry("layoutSetPrototype", "_layoutSetPrototypeLocalService"),
			Map.entry("mbCategory", "_mbCategoryLocalService"),
			Map.entry("organization", "_organizationLocalService"),
			Map.entry("role", "_roleLocalService"),
			Map.entry("thread", "_mbThreadLocalService"),
			Map.entry("user", "_userLocalService"),
			Map.entry("userGroup", "_userGroupLocalService"),
			Map.entry("vocabulary", "_assetVocabularyLocalService"));

		for (var method : CompanyScopedIds.class.getDeclaredMethods()) {
			if (!Modifier.isPublic(method.getModifiers()) ||
				(method.getParameterCount() != 3) ||
				(method.getParameterTypes()[1] != String.class)) {

				continue;
			}

			CompanyScopedIds ids = new CompanyScopedIds();
			List<String> calls = new ArrayList<>();
			String expectedField = serviceFields.get(method.getName());

			assertNotNull(expectedField, method.getName());

			for (Field field : CompanyScopedIds.class.getDeclaredFields()) {
				Class<?> serviceType = field.getType();
				Object service = Proxy.newProxyInstance(
					serviceType.getClassLoader(), new Class<?>[] {serviceType},
					(proxy, fetch, args) -> {
						calls.add(field.getName());

						if (fetch.getName().startsWith("fetch") &&
							(args != null) && (args.length > 0) &&
							Long.valueOf(20L).equals(args[0])) {

							return TestModelProxyUtil.proxy(
								fetch.getReturnType(), Map.of("getCompanyId", 10L));
						}

						return null;
					});

				field.setAccessible(true);
				field.set(ids, service);
			}

			assertDoesNotThrow(() -> method.invoke(ids, 10L, method.getName(), 20L));
			assertEquals(List.of(expectedField), calls, method.getName());
			calls.clear();

			var error = assertThrows(
				InvocationTargetException.class,
				() -> method.invoke(ids, 11L, method.getName(), 20L));

			assertInstanceOf(IllegalArgumentException.class, error.getCause());
			assertEquals(
				method.getName() + " 20 does not belong to the caller's company",
				error.getCause().getMessage());
			assertEquals(List.of(expectedField), calls, method.getName());
			calls.clear();

			for (Field field : CompanyScopedIds.class.getDeclaredFields()) {
				Class<?> serviceType = field.getType();

				field.setAccessible(true);
				field.set(
					ids,
					Proxy.newProxyInstance(
						serviceType.getClassLoader(), new Class<?>[] {serviceType},
						(proxy, fetch, args) -> {
							calls.add(field.getName());

							return null;
						}));
			}

			var missing = assertThrows(
				InvocationTargetException.class,
				() -> method.invoke(ids, 10L, method.getName(), 20L));

			assertInstanceOf(IllegalArgumentException.class, missing.getCause());
			assertEquals(
				method.getName() + " 20 does not exist",
				missing.getCause().getMessage());
			assertEquals(List.of(expectedField), calls, method.getName());
		}
	}

	@Test
	public void companyIsDerivedFromCreatorUser() throws Exception {
		CompanyScopedIds ids = new CompanyScopedIds();

		_setUserCompany(ids, 10L);

		assertEquals(10L, ids.companyId(30L));
	}

	@Test
	public void missingCreatorUserIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> validator(null).companyId(30L));
	}

	@Test
	public void webContentRejectsForeignSiteBeforeCreatingInFirstSite() throws Exception {
		CompanyScopedIds ids = validator(null);

		_setUserCompany(ids, 10L);
		_setField(
			ids, "_groupLocalService",
			Proxy.newProxyInstance(
				GroupLocalService.class.getClassLoader(),
				new Class<?>[] {GroupLocalService.class},
				(proxy, method, args) -> TestModelProxyUtil.proxy(
					Group.class,
					Map.of("getCompanyId", (long)args[0] == 20L ? 10L : 11L))));

		WebContentCreator creator = new WebContentCreator();

		_setField(creator, "_companyScopedIds", ids);
		WebContentBatchSpec spec = new WebContentBatchSpec(
			new BatchSpec(1, "article"), new long[] {20L, 21L}, 0L,
			new String[0], true, true, 0, "body", 0, 0, 0, "", 0L, 0L,
			AssetTagNames.EMPTY);

		// Creation services are deliberately unset: reaching creation would fail.
		IllegalArgumentException error = assertThrows(
			IllegalArgumentException.class, () -> creator.create(30L, spec, null));

		assertEquals("groupIds 21 does not belong to the caller's company", error.getMessage());
	}

	private static void _setField(Object target, String name, Object value)
		throws ReflectiveOperationException {

		Field field = target.getClass().getDeclaredField(name);

		field.setAccessible(true);
		field.set(target, value);
	}

	private static void _setUserCompany(CompanyScopedIds ids, long companyId)
		throws ReflectiveOperationException {

		_setField(
			ids, "_userLocalService",
			TestModelProxyUtil.proxy(
				UserLocalService.class,
				Map.of(
					"fetchUser",
					TestModelProxyUtil.proxy(
						User.class, Map.of("getCompanyId", companyId)))));
	}

	private CompanyScopedIds validator(Group group) {
		try {
			CompanyScopedIds ids = new CompanyScopedIds();

			_setField(
				ids, "_groupLocalService",
				TestModelProxyUtil.proxy(
					GroupLocalService.class,
					(group == null) ? Map.of() : Map.of("fetchGroup", group)));
			_setField(
				ids, "_userLocalService",
				TestModelProxyUtil.proxy(UserLocalService.class, Map.of()));

			return ids;
		}
		catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}

}
