package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
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
		for (var method : CompanyScopedIds.class.getDeclaredMethods()) {
			if (!Modifier.isPublic(method.getModifiers()) ||
				(method.getParameterCount() != 3) ||
				(method.getParameterTypes()[1] != String.class)) {

				continue;
			}

			CompanyScopedIds ids = new CompanyScopedIds();

			for (Field field : CompanyScopedIds.class.getDeclaredFields()) {
				Class<?> serviceType = field.getType();
				Object service = Proxy.newProxyInstance(
					serviceType.getClassLoader(), new Class<?>[] {serviceType},
					(proxy, fetch, args) -> TestModelProxyUtil.proxy(
						fetch.getReturnType(), Map.of("getCompanyId", 10L)));

				field.setAccessible(true);
				field.set(ids, service);
			}

			assertDoesNotThrow(() -> method.invoke(ids, 10L, method.getName(), 20L));
			var error = assertThrows(
				java.lang.reflect.InvocationTargetException.class,
				() -> method.invoke(ids, 11L, method.getName(), 20L));

			assertInstanceOf(IllegalArgumentException.class, error.getCause());

			for (Field field : CompanyScopedIds.class.getDeclaredFields()) {
				field.setAccessible(true);
				field.set(ids, TestModelProxyUtil.proxy(field.getType(), Map.of()));
			}

			var missing = assertThrows(
				java.lang.reflect.InvocationTargetException.class,
				() -> method.invoke(ids, 10L, method.getName(), 20L));

			assertInstanceOf(IllegalArgumentException.class, missing.getCause());
			assertTrue(missing.getCause().getMessage().contains("does not exist"));
		}
	}

	@Test
	public void companyIsDerivedFromCreatorUser() throws Exception {
		CompanyScopedIds ids = new CompanyScopedIds();
		Field field = CompanyScopedIds.class.getDeclaredField("_userLocalService");
		field.setAccessible(true);
		field.set(ids, TestModelProxyUtil.proxy(
			UserLocalService.class, Map.of("fetchUser",
				TestModelProxyUtil.proxy(User.class, Map.of("getCompanyId", 10L)))));

		assertEquals(10L, ids.companyId(30L));
	}

	@Test
	public void missingCreatorUserIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> validator(null).companyId(30L));
	}

	@Test
	public void webContentRejectsForeignSiteBeforeCreatingInFirstSite() throws Exception {
		CompanyScopedIds ids = validator(null);
		Field users = CompanyScopedIds.class.getDeclaredField("_userLocalService");
		users.setAccessible(true);
		users.set(ids, TestModelProxyUtil.proxy(
			UserLocalService.class, Map.of("fetchUser",
				TestModelProxyUtil.proxy(User.class, Map.of("getCompanyId", 10L)))));
		Field groups = CompanyScopedIds.class.getDeclaredField("_groupLocalService");
		groups.setAccessible(true);
		groups.set(ids, Proxy.newProxyInstance(
			GroupLocalService.class.getClassLoader(), new Class<?>[] {GroupLocalService.class},
			(proxy, method, args) -> TestModelProxyUtil.proxy(
				Group.class, Map.of("getCompanyId", (long)args[0] == 20L ? 10L : 11L))));

		WebContentCreator creator = new WebContentCreator();
		Field validator = WebContentCreator.class.getDeclaredField("_companyScopedIds");
		validator.setAccessible(true);
		validator.set(creator, ids);
		WebContentBatchSpec spec = new WebContentBatchSpec(
			new BatchSpec(1, "article"), new long[] {20L, 21L}, 0L,
			new String[0], true, true, 0, "body", 0, 0, 0, "", 0L, 0L,
			AssetTagNames.EMPTY);

		// Creation services are deliberately unset: reaching creation would fail.
		IllegalArgumentException error = assertThrows(
			IllegalArgumentException.class, () -> creator.create(30L, spec, null));

		assertEquals("groupIds 21 does not belong to the caller's company", error.getMessage());
	}

	private CompanyScopedIds validator(Group group) {
		try {
			CompanyScopedIds ids = new CompanyScopedIds();
			Field groups = CompanyScopedIds.class.getDeclaredField("_groupLocalService");
			groups.setAccessible(true);
			groups.set(ids, TestModelProxyUtil.proxy(
				GroupLocalService.class, group == null ? Map.of() : Map.of("fetchGroup", group)));
			Field users = CompanyScopedIds.class.getDeclaredField("_userLocalService");
			users.setAccessible(true);
			users.set(ids, TestModelProxyUtil.proxy(UserLocalService.class, Map.of()));

			return ids;
		}
		catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}

}
