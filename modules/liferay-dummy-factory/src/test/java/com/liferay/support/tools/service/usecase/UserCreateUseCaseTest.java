package com.liferay.support.tools.service.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.EmailDomain;
import com.liferay.support.tools.service.UserBatchSpec;
import com.liferay.support.tools.service.UserCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UserCreateUseCaseTest {

	@Test
	void createReturnsGroupIdZeroWhenGenerateLayoutsFalse() throws Throwable {
		User mockUser = _user("user@test.com", "user1", 101L);

		UserCreateUseCase useCase = new UserCreateUseCase(
			_neverCalledLayoutSetLocalService(),
			_stubUserCreator(List.of(mockUser), 1));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(1, false), ProgressCallback.NOOP);

		assertTrue(result.success());
		UserItemResult item = result.items().get(0);
		assertEquals(0L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createReturnsGroupIdZeroWhenPersonalSiteIsNull() throws Throwable {
		User mockUser = _user("user@test.com", "user1", 101L);

		UserCreateUseCase useCase = new UserCreateUseCase(
			_neverCalledLayoutSetLocalService(),
			_stubUserCreator(List.of(mockUser), 1));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(1, true), ProgressCallback.NOOP);

		assertTrue(result.success());
		UserItemResult item = result.items().get(0);
		assertEquals(0L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createPopulatesGroupIdAndUuidsWhenLayoutsGenerated() throws Throwable {
		Group mockGroup = TestModelProxyUtil.proxy(
			Group.class, Map.of("getGroupId", 201L));
		LayoutSet mockPublicLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class,
			Map.of("getLayoutSetPrototypeUuid", "pub-uuid-1"));
		LayoutSet mockPrivateLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class,
			Map.of("getLayoutSetPrototypeUuid", "priv-uuid-1"));

		User mockUser = _userWithGroup("user@test.com", "user1", 101L, mockGroup);

		UserCreateUseCase useCase = new UserCreateUseCase(
			_layoutSetLocalService(mockPublicLayoutSet, mockPrivateLayoutSet),
			_stubUserCreator(List.of(mockUser), 1));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(1, true), ProgressCallback.NOOP);

		assertTrue(result.success());
		UserItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertEquals("pub-uuid-1", item.publicLayoutSetPrototypeUuid());
		assertEquals("priv-uuid-1", item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createNormalizesEmptyStringUuidToNull() throws Throwable {
		Group mockGroup = TestModelProxyUtil.proxy(
			Group.class, Map.of("getGroupId", 201L));
		LayoutSet mockPublicLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class, Map.of("getLayoutSetPrototypeUuid", ""));
		LayoutSet mockPrivateLayoutSet = TestModelProxyUtil.proxy(
			LayoutSet.class, Map.of("getLayoutSetPrototypeUuid", ""));

		User mockUser = _userWithGroup("user@test.com", "user1", 101L, mockGroup);

		UserCreateUseCase useCase = new UserCreateUseCase(
			_layoutSetLocalService(mockPublicLayoutSet, mockPrivateLayoutSet),
			_stubUserCreator(List.of(mockUser), 1));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(1, true), ProgressCallback.NOOP);

		assertTrue(result.success());
		UserItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createHandlesNullLayoutSetsGracefully() throws Throwable {
		Group mockGroup = TestModelProxyUtil.proxy(
			Group.class, Map.of("getGroupId", 201L));

		User mockUser = _userWithGroup("user@test.com", "user1", 101L, mockGroup);

		UserCreateUseCase useCase = new UserCreateUseCase(
			_layoutSetLocalService(null, null),
			_stubUserCreator(List.of(mockUser), 1));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(1, true), ProgressCallback.NOOP);

		assertTrue(result.success());
		UserItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createHandlesLayoutSetFetchException() throws Throwable {
		Group mockGroup = TestModelProxyUtil.proxy(
			Group.class, Map.of("getGroupId", 201L));

		User mockUser = _userWithGroup("user@test.com", "user1", 101L, mockGroup);

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

		UserCreateUseCase useCase = new UserCreateUseCase(
			throwingService, _stubUserCreator(List.of(mockUser), 1));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(1, true), ProgressCallback.NOOP);

		assertTrue(result.success());
		UserItemResult item = result.items().get(0);
		assertEquals(201L, item.groupId());
		assertNull(item.publicLayoutSetPrototypeUuid());
		assertNull(item.privateLayoutSetPrototypeUuid());
	}

	@Test
	void createPropagatesFailureWhenCreatorFails() throws Throwable {
		User mockUser = _user("user@test.com", "user1", 101L);

		UserCreateUseCase useCase = new UserCreateUseCase(
			_neverCalledLayoutSetLocalService(),
			_failingUserCreator(List.of(mockUser), 2, "Only 1 of 2 users created"));

		BatchResult<UserItemResult> result = useCase.create(
			1L, 10L, _userBatchSpec(2, false), ProgressCallback.NOOP);

		assertFalse(result.success());
		assertEquals(2, result.requested());
		assertEquals(1, result.skipped());
		assertEquals("Only 1 of 2 users created", result.error());
		assertEquals(1, result.items().size());
	}

	private static User _user(
		String emailAddress, String screenName, long userId) {

		return TestModelProxyUtil.proxy(
			User.class,
			Map.of(
				"getEmailAddress", emailAddress,
				"getScreenName", screenName,
				"getUserId", userId));
	}

	private static User _userWithGroup(
		String emailAddress, String screenName, long userId, Group group) {

		Map<String, Object> values = new java.util.HashMap<>();

		values.put("getEmailAddress", emailAddress);
		values.put("getGroup", group);
		values.put("getScreenName", screenName);
		values.put("getUserId", userId);

		return TestModelProxyUtil.proxy(User.class, values);
	}

	private static LayoutSetLocalService _neverCalledLayoutSetLocalService() {
		return (LayoutSetLocalService)Proxy.newProxyInstance(
			LayoutSetLocalService.class.getClassLoader(),
			new Class<?>[] {LayoutSetLocalService.class},
			(proxy, method, args) -> {
				if ("fetchLayoutSet".equals(method.getName())) {
					throw new AssertionError(
						"fetchLayoutSet must not be called");
				}

				return null;
			});
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

	private static UserBatchSpec _userBatchSpec(
		int count, boolean generateLayouts) {

		return new UserBatchSpec(
			new BatchSpec(count, "testuser"),
			EmailDomain.of(null),
			"test",
			true,
			"",
			new long[0],
			new long[0],
			new long[0],
			new long[0],
			new long[0],
			false,
			"en_US",
			generateLayouts,
			0L,
			0L,
			new long[0]);
	}

	private static UserCreator _stubUserCreator(
		List<User> users, int requested) {

		return new UserCreator() {

			@Override
			public BatchResult<User> create(
				long creatorUserId, long companyId, UserBatchSpec spec,
				ProgressCallback progress) {

				return BatchResult.success(requested, users, 0);
			}

		};
	}

	private static UserCreator _failingUserCreator(
		List<User> users, int requested, String error) {

		return new UserCreator() {

			@Override
			public BatchResult<User> create(
				long creatorUserId, long companyId, UserBatchSpec spec,
				ProgressCallback progress) {

				return BatchResult.failure(
					requested, users, requested - users.size(), error);
			}

		};
	}

}
