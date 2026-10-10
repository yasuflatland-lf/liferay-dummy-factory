package com.liferay.support.tools.security;

import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;

import java.util.Collection;

public final class DataCreationAuthorization {

	public static String forbiddenReason(
		boolean companyAdmin, boolean omniadmin, Collection<String> operations) {

		if (!companyAdmin) {
			return "Executing a workflow requires a company administrator.";
		}

		if (!omniadmin && operations.contains(_COMPANY_CREATE_OPERATION)) {
			return "company.create requires an omniadmin.";
		}

		return null;
	}

	public static void requirePermission(
			PermissionChecker permissionChecker, String command)
		throws PrincipalException {

		if (!permissionChecker.isCompanyAdmin()) {
			throw new PrincipalException.MustBeCompanyAdmin(permissionChecker);
		}

		if (!permissionChecker.isOmniadmin() &&
			_COMPANY_CREATE_COMMAND.equals(command)) {

			throw new PrincipalException.MustBeOmniadmin(permissionChecker);
		}
	}

	private DataCreationAuthorization() {
	}

	private static final String _COMPANY_CREATE_COMMAND = "/ldf/company";

	private static final String _COMPANY_CREATE_OPERATION = "company.create";

}
