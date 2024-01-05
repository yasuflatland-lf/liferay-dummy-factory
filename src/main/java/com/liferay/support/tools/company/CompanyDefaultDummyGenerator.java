package com.liferay.support.tools.company;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.instances.service.PortalInstancesLocalService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;

/**
 * Company Generator
 *
 * @author Yasuyuki Takeo
 */
@Component(immediate = true, service = CompanyDefaultDummyGenerator.class)
public class CompanyDefaultDummyGenerator extends DummyGenerator<CompanyContext> {

  private static final Log _log = LogFactoryUtil.getLog(CompanyDefaultDummyGenerator.class);

  @Reference
  private CompanyService _companyService;

  @Reference
  private PortalInstancesLocalService _portalInstancesLocalService;

  @Override
  protected CompanyContext getContext(ActionRequest request) throws Exception {
    return new CompanyContext(request);
  }

  @Override
  protected void exec(ActionRequest request, CompanyContext paramContext)
      throws Exception {

    //Tracking progress start
    ProgressManager progressManager = new ProgressManager();
    progressManager.start(request);

    System.out.println("Starting to create " + paramContext.getNumberOfCompanies() + " companies");

    for (long i = 1; i <= paramContext.getNumberOfCompanies(); i++) {
      //Update progress
      progressManager.trackProgress(i, paramContext.getNumberOfCompanies());

      //Create company web id
      StringBundler webId = new StringBundler(2);

      //Create company Virtual Host Name
      StringBundler virtualHostname = new StringBundler(2);

      //Create company Mail Domain
      StringBundler mx = new StringBundler(2);

      //Add number more than one company
      if (1 < paramContext.getNumberOfCompanies()) {
        webId.append(String.valueOf(i)).append(paramContext.getWebId());
        virtualHostname.append(String.valueOf(i)).append(paramContext.getVirtualHostname());
        mx.append(String.valueOf(i)).append(paramContext.getMx());
      } else {
        webId.append(paramContext.getWebId());
        virtualHostname.append(paramContext.getVirtualHostname());
        mx.append(paramContext.getMx());
      }

      try {
        if (_log.isDebugEnabled()) {
          _log.debug("-----");
        }

        Company company = _companyService.addCompany(
            0L,
            webId.toString(),
            virtualHostname.toString(),
            mx.toString(),
            paramContext.getMaxUsers(),
            paramContext.isActive());

      } catch (Throwable e) {
        _log.error(e,e);
      }
    }

    //Finish progress
    progressManager.finish();

    System.out.println("Finished creating " + paramContext.getNumberOfCompanies() + " companies");

  }
}