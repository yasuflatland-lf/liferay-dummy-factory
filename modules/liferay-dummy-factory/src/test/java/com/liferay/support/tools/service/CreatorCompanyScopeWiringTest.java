package com.liferay.support.tools.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CreatorCompanyScopeWiringTest {

	@Test
	public void blogsChecksRequiredGroupEvenWhenZero() throws Exception {
		for (long groupId : new long[] {20L, 0L}) {
			Call expected = new Call("group", "groupId", groupId);
			RecordingIds ids = new RecordingIds(expected);
			BlogsCreator creator = new BlogsCreator();

			_inject(creator, ids);

			assertSame(
				ids._marker,
				assertThrows(
					IllegalArgumentException.class,
					() -> creator.create(
						30L, new BlogsBatchSpec(
							new BatchSpec(1, "blog"), groupId, "body", "", "",
							false, false, null), null)));
			assertEquals(List.of(expected), ids._calls);
			assertEquals(List.of(30L), ids._creatorUserIds);
		}
	}

	@Test
	public void organizationChecksParent() throws Exception {
		Call expected = new Call("organization", "parentOrganizationId", 20L);
		RecordingIds ids = new RecordingIds(expected);
		OrganizationCreator creator = new OrganizationCreator();

		_inject(creator, ids);

		assertSame(
			ids._marker,
			assertThrows(
				IllegalArgumentException.class,
				() -> creator.create(30L, new BatchSpec(1, "org"), 20L, false, null)));
		assertEquals(List.of(expected), ids._calls);
		assertEquals(List.of(30L), ids._creatorUserIds);
	}

	@Test
	public void organizationSkipsZeroParent() throws Exception {
		RecordingIds ids = new RecordingIds(null);
		OrganizationCreator creator = new OrganizationCreator();

		_inject(creator, ids);

		// A null batch stops execution immediately after the ID checks.
		assertThrows(
			NullPointerException.class,
			() -> creator.create(30L, null, 0L, false, null));
		assertEquals(List.of(), ids._calls);
		assertEquals(List.of(), ids._creatorUserIds);
	}

	@Test
	public void siteChecksParentAndAllTemplates() throws Exception {
		Call last = new Call("layoutSetPrototype", "privateLayoutSetPrototypeId", 23L);
		RecordingIds ids = new RecordingIds(last);
		SiteCreator creator = new SiteCreator();

		_inject(creator, ids);

		assertSame(
			ids._marker,
			assertThrows(
				IllegalArgumentException.class,
				() -> creator.create(
					30L, 10L, new BatchSpec(1, "site"), SiteMembershipType.OPEN,
					20L, 21L, true, false, true, "", 22L, 23L, null)));
		assertEquals(
			List.of(
				new Call("group", "parentGroupId", 20L),
				new Call("layoutSetPrototype", "siteTemplateId", 21L),
				new Call("layoutSetPrototype", "publicLayoutSetPrototypeId", 22L),
				last),
			ids._calls);
	}

	@Test
	public void siteSkipsZeroParentAndTemplates() throws Exception {
		RecordingIds ids = new RecordingIds(null);
		SiteCreator creator = new SiteCreator();

		_inject(creator, ids);

		assertThrows(
			NullPointerException.class,
			() -> creator.create(
				30L, 10L, null, SiteMembershipType.OPEN, 0L, 0L,
				true, false, true, "", 0L, 0L, null));
		assertEquals(List.of(), ids._calls);
	}

	@Test
	public void userChecksMembershipsAndPersonalSiteTemplates() throws Exception {
		Call last = new Call("layoutSetPrototype", "privateLayoutSetPrototypeId", 27L);
		RecordingIds ids = new RecordingIds(last);
		UserCreator creator = new UserCreator();

		_inject(creator, ids);

		assertSame(
			ids._marker,
			assertThrows(
				IllegalArgumentException.class,
				() -> creator.create(30L, 10L, _userSpec(26L, 27L), null)));
		List<Call> expected = new ArrayList<>(_memberships());

		expected.add(new Call("layoutSetPrototype", "publicLayoutSetPrototypeId", 26L));
		expected.add(last);
		assertEquals(expected, ids._calls);
	}

	@Test
	public void userSkipsZeroTemplatesButChecksEveryMembershipId() throws Exception {
		RecordingIds ids = new RecordingIds(null);
		UserCreator creator = new UserCreator();

		_inject(creator, ids);

		assertThrows(
			NullPointerException.class,
			() -> creator.create(30L, 10L, _userSpec(0L, 0L), null));
		assertEquals(_memberships(), ids._calls);
	}

	private static void _inject(Object creator, CompanyScopedIds ids)
		throws ReflectiveOperationException {

		Field field = creator.getClass().getDeclaredField("_companyScopedIds");

		field.setAccessible(true);
		field.set(creator, ids);
	}

	private static List<Call> _memberships() {
		return List.of(
			new Call("organization", "organizationIds", 20L),
			new Call("organization", "organizationIds", 0L),
			new Call("role", "roleIds", 21L),
			new Call("role", "roleIds", 0L),
			new Call("role", "siteRoleIds", 22L),
			new Call("role", "siteRoleIds", 0L),
			new Call("role", "orgRoleIds", 23L),
			new Call("role", "orgRoleIds", 0L),
			new Call("userGroup", "userGroupIds", 24L),
			new Call("userGroup", "userGroupIds", 0L),
			new Call("group", "groupIds", 25L),
			new Call("group", "groupIds", 0L));
	}

	private static UserBatchSpec _userSpec(long publicId, long privateId) {
		return new UserBatchSpec(
			null, EmailDomain.of("liferay.com"), "test", true, "",
			new long[] {20L, 0L}, new long[] {21L, 0L}, new long[] {24L, 0L},
			new long[] {22L, 0L}, new long[] {23L, 0L}, false, "en_US", true,
			publicId, privateId, new long[] {25L, 0L});
	}

	private record Call(String method, String parameter, long id) {
	}

	private static class RecordingIds extends CompanyScopedIds {

		public RecordingIds(Call last) {
			_last = last;
		}

		@Override
		public long companyId(long userId) {
			_creatorUserIds.add(userId);

			return 10L;
		}

		@Override
		public void group(long companyId, String parameter, long id) {
			_record(companyId, "group", parameter, id);
		}

		@Override
		public void layoutSetPrototype(long companyId, String parameter, long id) {
			_record(companyId, "layoutSetPrototype", parameter, id);
		}

		@Override
		public void organization(long companyId, String parameter, long id) {
			_record(companyId, "organization", parameter, id);
		}

		@Override
		public void role(long companyId, String parameter, long id) {
			_record(companyId, "role", parameter, id);
		}

		@Override
		public void userGroup(long companyId, String parameter, long id) {
			_record(companyId, "userGroup", parameter, id);
		}

		private void _record(
			long companyId, String method, String parameter, long id) {
			assertEquals(10L, companyId);

			Call call = new Call(method, parameter, id);

			_calls.add(call);

			if (call.equals(_last)) {
				throw _marker;
			}
		}

		private final List<Call> _calls = new ArrayList<>();
		private final List<Long> _creatorUserIds = new ArrayList<>();
		private final Call _last;
		private final IllegalArgumentException _marker = new IllegalArgumentException(
			"ID checks complete");

	}

}
