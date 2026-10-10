package com.liferay.support.tools.security;

import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;

import java.util.Collection;

/**
 * The one data-creation rule for the REST/MCP and portlet entry points: the
 * caller must be a company administrator, and creating companies also requires
 * an omniadmin. {@link #forbiddenReason} serves REST, which answers with a
 * reason string; {@link #requirePermission} serves the portlet, which throws a
 * {@link PrincipalException}.
 */
public final class DataCreationAuthorization {

	public static String forbiddenReason(
		boolean companyAdmin, boolean omniadmin, Collection<String> operations) {

		Denial denial = _denial(
			companyAdmin, omniadmin, operations.contains("company.create"));

		if (denial == Denial.COMPANY_ADMIN_REQUIRED) {
			return "Executing a workflow requires a company administrator.";
		}

		if (denial == Denial.OMNIADMIN_REQUIRED) {
			return "company.create requires an omniadmin.";
		}

		return null;
	}

	/**
	 * @param createsCompany whether the calling command creates companies. The
	 *        command passes this itself; it must never be derived from the
	 *        request's resource ID, which the client controls.
	 */
	public static void requirePermission(
			PermissionChecker permissionChecker, boolean createsCompany)
		throws PrincipalException {

		Denial denial = _denial(
			permissionChecker.isCompanyAdmin(), permissionChecker.isOmniadmin(),
			createsCompany);

		if (denial == Denial.COMPANY_ADMIN_REQUIRED) {
			throw new PrincipalException.MustBeCompanyAdmin(permissionChecker);
		}

		if (denial == Denial.OMNIADMIN_REQUIRED) {
			throw new PrincipalException.MustBeOmniadmin(permissionChecker);
		}
	}

	private static Denial _denial(
		boolean companyAdmin, boolean omniadmin, boolean createsCompany) {

		if (!companyAdmin) {
			return Denial.COMPANY_ADMIN_REQUIRED;
		}

		if (createsCompany && !omniadmin) {
			return Denial.OMNIADMIN_REQUIRED;
		}

		return null;
	}

	private DataCreationAuthorization() {
	}

	private enum Denial {

		COMPANY_ADMIN_REQUIRED, OMNIADMIN_REQUIRED

	}

}
