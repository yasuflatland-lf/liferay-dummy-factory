package com.liferay.support.tools.security;

import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;

import java.util.Collection;
import java.util.List;

public final class DataCreationAuthorization {

	public static String forbiddenReason(
		boolean companyAdmin, boolean omniadmin, Collection<String> operations) {

		if (!companyAdmin) {
			return "Executing a workflow requires a company administrator.";
		}

		if (!omniadmin && operations.contains("company.create")) {
			return "company.create requires an omniadmin.";
		}

		return null;
	}

	public static void requirePermission(
			PermissionChecker permissionChecker, String command)
		throws PrincipalException {

		String reason = forbiddenReason(
			permissionChecker.isCompanyAdmin(), permissionChecker.isOmniadmin(),
			List.of("/ldf/company".equals(command) ? "company.create" : command));

		if (reason == null) {
			return;
		}

		if (permissionChecker.isCompanyAdmin()) {
			throw new PrincipalException.MustBeOmniadmin(permissionChecker);
		}

		throw new PrincipalException.MustBeCompanyAdmin(permissionChecker);
	}

	private DataCreationAuthorization() {
	}

}
