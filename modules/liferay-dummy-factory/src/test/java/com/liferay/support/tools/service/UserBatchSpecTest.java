package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserBatchSpecTest {

	@Test
	void customValuesArePreserved() {
		long[] orgIds = {1L, 2L};
		long[] roleIds = {3L};
		long[] userGroupIds = {4L};
		long[] siteRoleIds = {5L};
		long[] orgRoleIds = {6L};
		long[] groupIds = {7L, 8L, 9L};

		UserBatchSpec spec = _builder()
			.emailDomain("example.com")
			.password("secret")
			.male(false)
			.jobTitle("Engineer")
			.organizationIds(orgIds)
			.roleIds(roleIds)
			.userGroupIds(userGroupIds)
			.siteRoleIds(siteRoleIds)
			.orgRoleIds(orgRoleIds)
			.fakerEnable(true)
			.locale("ja_JP")
			.generatePersonalSiteLayouts(true)
			.publicLayoutSetPrototypeId(100L)
			.privateLayoutSetPrototypeId(200L)
			.groupIds(groupIds)
			.build();

		assertEquals("example.com", spec.emailDomain().value());
		assertEquals("secret", spec.password());
		assertEquals(false, spec.male());
		assertEquals("Engineer", spec.jobTitle());
		assertArrayEquals(orgIds, spec.organizationIds());
		assertArrayEquals(roleIds, spec.roleIds());
		assertArrayEquals(userGroupIds, spec.userGroupIds());
		assertArrayEquals(siteRoleIds, spec.siteRoleIds());
		assertArrayEquals(orgRoleIds, spec.orgRoleIds());
		assertEquals(true, spec.fakerEnable());
		assertEquals("ja_JP", spec.locale());
		assertEquals(true, spec.generatePersonalSiteLayouts());
		assertEquals(100L, spec.publicLayoutSetPrototypeId());
		assertEquals(200L, spec.privateLayoutSetPrototypeId());
		assertArrayEquals(groupIds, spec.groupIds());
	}

	@Test
	void nullEmailDomainDefaultsToLiferayCom() {
		UserBatchSpec spec = _builder().emailDomain(null).build();

		assertEquals("liferay.com", spec.emailDomain().value());
	}

	@Test
	void emptyEmailDomainDefaultsToLiferayCom() {
		UserBatchSpec spec = _builder().emailDomain("").build();

		assertEquals("liferay.com", spec.emailDomain().value());
	}

	@Test
	void nullPasswordDefaultsToTest() {
		UserBatchSpec spec = _builder().password(null).build();

		assertEquals("test", spec.password());
	}

	@Test
	void emptyPasswordDefaultsToTest() {
		UserBatchSpec spec = _builder().password("").build();

		assertEquals("test", spec.password());
	}

	@Test
	void nullLocaleDefaultsToEnUs() {
		UserBatchSpec spec = _builder().locale(null).build();

		assertEquals("en_US", spec.locale());
	}

	@Test
	void emptyLocaleDefaultsToEnUs() {
		UserBatchSpec spec = _builder().locale("").build();

		assertEquals("en_US", spec.locale());
	}

	@Test
	void nullJobTitleDefaultsToEmptyString() {
		UserBatchSpec spec = _builder().jobTitle(null).build();

		assertEquals("", spec.jobTitle());
	}

	@Test
	void nullOrganizationIdsDefaultsToEmptyArray() {
		UserBatchSpec spec = _builder().organizationIds(null).build();

		assertArrayEquals(new long[0], spec.organizationIds());
	}

	@Test
	void nullRoleIdsDefaultsToEmptyArray() {
		UserBatchSpec spec = _builder().roleIds(null).build();

		assertArrayEquals(new long[0], spec.roleIds());
	}

	@Test
	void nullUserGroupIdsDefaultsToEmptyArray() {
		UserBatchSpec spec = _builder().userGroupIds(null).build();

		assertArrayEquals(new long[0], spec.userGroupIds());
	}

	@Test
	void nullSiteRoleIdsDefaultsToEmptyArray() {
		UserBatchSpec spec = _builder().siteRoleIds(null).build();

		assertArrayEquals(new long[0], spec.siteRoleIds());
	}

	@Test
	void nullOrgRoleIdsDefaultsToEmptyArray() {
		UserBatchSpec spec = _builder().orgRoleIds(null).build();

		assertArrayEquals(new long[0], spec.orgRoleIds());
	}

	@Test
	void nullGroupIdsDefaultsToEmptyArray() {
		UserBatchSpec spec = _builder().groupIds(null).build();

		assertArrayEquals(new long[0], spec.groupIds());
	}

	@Test
	void composedBatchSpecZeroCountThrows() {
		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(0, "Test")).build());
	}

	@Test
	void composedBatchSpecEmptyBaseNameThrows() {
		assertThrows(
			IllegalArgumentException.class,
			() -> _builder().batch(new BatchSpec(1, "")).build());
	}

	private static _Builder _builder() {
		return new _Builder();
	}

	private static class _Builder {

		_Builder batch(BatchSpec batch) {
			_batch = batch;
			return this;
		}

		UserBatchSpec build() {
			return new UserBatchSpec(
				_batch, _emailDomain, _password, _male, _jobTitle,
				_organizationIds, _roleIds, _userGroupIds,
				_siteRoleIds, _orgRoleIds, _fakerEnable, _locale,
				_generatePersonalSiteLayouts, _publicLayoutSetPrototypeId,
				_privateLayoutSetPrototypeId, _groupIds);
		}

		_Builder emailDomain(String emailDomain) {
			_emailDomain = EmailDomain.of(emailDomain);
			return this;
		}

		_Builder fakerEnable(boolean fakerEnable) {
			_fakerEnable = fakerEnable;
			return this;
		}

		_Builder generatePersonalSiteLayouts(
			boolean generatePersonalSiteLayouts) {

			_generatePersonalSiteLayouts = generatePersonalSiteLayouts;
			return this;
		}

		_Builder groupIds(long[] groupIds) {
			_groupIds = groupIds;
			return this;
		}

		_Builder jobTitle(String jobTitle) {
			_jobTitle = jobTitle;
			return this;
		}

		_Builder locale(String locale) {
			_locale = locale;
			return this;
		}

		_Builder male(boolean male) {
			_male = male;
			return this;
		}

		_Builder organizationIds(long[] organizationIds) {
			_organizationIds = organizationIds;
			return this;
		}

		_Builder orgRoleIds(long[] orgRoleIds) {
			_orgRoleIds = orgRoleIds;
			return this;
		}

		_Builder password(String password) {
			_password = password;
			return this;
		}

		_Builder privateLayoutSetPrototypeId(
			long privateLayoutSetPrototypeId) {

			_privateLayoutSetPrototypeId = privateLayoutSetPrototypeId;
			return this;
		}

		_Builder publicLayoutSetPrototypeId(
			long publicLayoutSetPrototypeId) {

			_publicLayoutSetPrototypeId = publicLayoutSetPrototypeId;
			return this;
		}

		_Builder roleIds(long[] roleIds) {
			_roleIds = roleIds;
			return this;
		}

		_Builder siteRoleIds(long[] siteRoleIds) {
			_siteRoleIds = siteRoleIds;
			return this;
		}

		_Builder userGroupIds(long[] userGroupIds) {
			_userGroupIds = userGroupIds;
			return this;
		}

		private BatchSpec _batch = new BatchSpec(1, "test");
		private EmailDomain _emailDomain = EmailDomain.of("liferay.com");
		private boolean _fakerEnable = false;
		private boolean _generatePersonalSiteLayouts = false;
		private long[] _groupIds = new long[0];
		private String _jobTitle = "";
		private String _locale = "en_US";
		private boolean _male = true;
		private long[] _organizationIds = new long[0];
		private long[] _orgRoleIds = new long[0];
		private String _password = "test";
		private long _privateLayoutSetPrototypeId = 0L;
		private long _publicLayoutSetPrototypeId = 0L;
		private long[] _roleIds = new long[0];
		private long[] _siteRoleIds = new long[0];
		private long[] _userGroupIds = new long[0];

	}

}
