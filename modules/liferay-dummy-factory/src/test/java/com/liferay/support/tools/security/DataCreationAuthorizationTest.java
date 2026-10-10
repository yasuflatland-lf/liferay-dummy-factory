package com.liferay.support.tools.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DataCreationAuthorizationTest {

	@Test
	void nonAdminIsForbidden() {
		assertEquals(
			"Executing a workflow requires a company administrator.",
			DataCreationAuthorization.forbiddenReason(
				false, false, List.of("role.create")));
	}

	@Test
	void omniadminWithoutCompanyAdminIsForbidden() {
		assertEquals(
			"Executing a workflow requires a company administrator.",
			DataCreationAuthorization.forbiddenReason(
				false, true, List.of("role.create")));
	}

	@Test
	void companyAdminCanCreateRoles() {
		assertNull(DataCreationAuthorization.forbiddenReason(
			true, false, List.of("role.create")));
	}

	@Test
	void companyAdminCannotCreateCompanies() {
		assertEquals(
			"company.create requires an omniadmin.",
			DataCreationAuthorization.forbiddenReason(
				true, false, List.of("company.create")));
	}

	@Test
	void companyAdminCannotCreateCompaniesInLaterSteps() {
		assertEquals(
			"company.create requires an omniadmin.",
			DataCreationAuthorization.forbiddenReason(
				true, false, List.of("role.create", "company.create")));
	}

	@Test
	void omniadminCanCreateCompanies() {
		assertNull(DataCreationAuthorization.forbiddenReason(
			true, true, List.of("role.create", "company.create")));
	}

	@Test
	void companyAdminAllowsMissingOperationsUntilValidation() {
		assertNull(DataCreationAuthorization.forbiddenReason(
			true, false, Arrays.asList(null, "role.create")));
	}

	@Test
	void portletNonAdminIsRejected() {
		assertThrows(
			PrincipalException.MustBeCompanyAdmin.class,
			() -> DataCreationAuthorization.requirePermission(
				_permissionChecker(false, false), false));
	}

	@Test
	void portletNonAdminCreatingCompaniesNeedsCompanyAdminFirst() {
		assertThrows(
			PrincipalException.MustBeCompanyAdmin.class,
			() -> DataCreationAuthorization.requirePermission(
				_permissionChecker(false, false), true));
	}

	@Test
	void portletOmniadminWithoutCompanyAdminIsRejected() {
		assertThrows(
			PrincipalException.MustBeCompanyAdmin.class,
			() -> DataCreationAuthorization.requirePermission(
				_permissionChecker(false, true), false));
	}

	@Test
	void portletCompanyAdminCanCreateData() {
		assertDoesNotThrow(
			() -> DataCreationAuthorization.requirePermission(
				_permissionChecker(true, false), false));
	}

	@Test
	void portletCompanyAdminCannotCreateCompanies() {
		assertThrows(
			PrincipalException.MustBeOmniadmin.class,
			() -> DataCreationAuthorization.requirePermission(
				_permissionChecker(true, false), true));
	}

	@Test
	void portletOmniadminCanCreateCompanies() {
		assertDoesNotThrow(
			() -> DataCreationAuthorization.requirePermission(
				_permissionChecker(true, true), true));
	}

	private PermissionChecker _permissionChecker(
		boolean companyAdmin, boolean omniadmin) {

		return (PermissionChecker)Proxy.newProxyInstance(
			PermissionChecker.class.getClassLoader(),
			new Class<?>[] {PermissionChecker.class},
			(proxy, method, args) -> {
				if (method.getReturnType() == long.class) {
					return 42L;
				}

				if (method.getName().equals("isCompanyAdmin")) {
					return companyAdmin;
				}

				return method.getName().equals("isOmniadmin") && omniadmin;
			});
	}

}
