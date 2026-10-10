package com.liferay.support.tools.portlet.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.support.tools.security.DataCreationAuthorization;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

class CompanyResourceCommandTest {

	@Test
	void nonOmniadminIsRejected() {
		PermissionChecker permissionChecker = _permissionChecker(false);

		assertThrows(
			PrincipalException.MustBeOmniadmin.class,
			() -> DataCreationAuthorization.requirePermission(
				permissionChecker, "/ldf/company"));
	}

	@Test
	void omniadminPasses() {
		PermissionChecker permissionChecker = _permissionChecker(true);

		assertDoesNotThrow(
			() -> DataCreationAuthorization.requirePermission(
				permissionChecker, "/ldf/company"));
	}

	private PermissionChecker _permissionChecker(boolean omniadmin) {
		return PermissionChecker.class.cast(
			Proxy.newProxyInstance(
				PermissionChecker.class.getClassLoader(),
				new Class<?>[] {PermissionChecker.class},
				(proxy, method, args) -> {
					if (method.getName().equals("isCompanyAdmin")) {
						return true;
					}

					if (method.getName().equals("isOmniadmin")) {
						return omniadmin;
					}

					Class<?> returnType = method.getReturnType();

					if (returnType == boolean.class) {
						return false;
					}

					if (returnType == int.class) {
						return 0;
					}

					if (returnType == long.class) {
						return 0L;
					}

					return null;
				}));
	}

}
