package com.liferay.support.tools.sap;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.security.service.access.policy.model.SAPEntry;
import com.liferay.portal.security.service.access.policy.service.SAPEntryLocalService;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * Integration-test only. Widens the SYSTEM_USER_PASSWORD and SYSTEM_DEFAULT
 * SAP rows to {@code enabled=true, allowedServiceSignatures="*"} so that
 * BasicAuth-authenticated JSONWS calls from the Spock suite are not blocked
 * by the persisted whitelist.
 *
 * DXP 2026 persists SAP entries in the SAPEntry table and
 * {@code SAPServiceVerifyProcess} only creates missing rows — it never
 * updates existing ones. An {@code OSGi .config} override of
 * {@code SAPConfiguration} therefore has no effect once the rows are in
 * place. The only reliable fix is an in-process mutation via
 * {@link SAPEntryLocalService#updateSAPEntry}, which is a local service call
 * and not subject to HTTP-layer SAP enforcement.
 *
 * Gated by {@link ConfigurationPolicy#REQUIRE}: the component only activates
 * when an {@code osgi/configs/com.liferay.support.tools.sap.SAPTestSetup.config}
 * file is present. Production deploys that do not ship this file are
 * unaffected.
 */
@Component(
	configurationPid = "com.liferay.support.tools.sap.SAPTestSetup",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	immediate = true,
	service = {}
)
public class SAPTestSetup {

	@Activate
	protected void activate() {
		_log.info("SAPTestSetup activating — widening system SAP entries");

		List<Company> companies = _companyLocalService.getCompanies();

		_log.info("Found " + companies.size() + " companies");

		for (Company company : companies) {
			long companyId = company.getCompanyId();

			try {
				_sapEntryLocalService.checkSystemSAPEntries(companyId);
			}
			catch (PortalException portalException) {
				_log.warn(
					"checkSystemSAPEntries failed for company " + companyId,
					portalException);
			}

			_widenSystemEntry(companyId, "SYSTEM_USER_PASSWORD");
			_widenSystemEntry(companyId, "SYSTEM_DEFAULT");
		}
	}

	private void _widenSystemEntry(long companyId, String name) {
		SAPEntry entry = _sapEntryLocalService.fetchSAPEntry(companyId, name);

		if (entry == null) {
			_log.error(
				"[DIAG] SAP entry " + name + " NOT FOUND for company " +
					companyId);

			return;
		}

		try {
			_sapEntryLocalService.updateSAPEntry(
				entry.getSapEntryId(), "*", entry.isDefaultSAPEntry(), true,
				entry.getName(), entry.getTitleMap(), new ServiceContext());
		}
		catch (PortalException portalException) {
			_log.error(
				"Unable to widen SAP entry " + name + " for company " +
					companyId,
				portalException);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(SAPTestSetup.class);

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference(target = "(module.service.lifecycle=portal.initialized)")
	private ModuleServiceLifecycle _moduleServiceLifecycle;

	@Reference
	private SAPEntryLocalService _sapEntryLocalService;

}
