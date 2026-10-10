package com.liferay.support.tools.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvoker;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyCreatorTest {

	@BeforeEach
	void setUp() {
		new TransactionInvokerUtil().setTransactionInvoker(
			new TransactionInvoker() {

				@Override
				public <T> T invoke(
						TransactionConfig transactionConfig, Callable<T> callable)
					throws Throwable {

					return callable.call();
				}

			});
	}

	@AfterEach
	void tearDown() {
		new TransactionInvokerUtil().setTransactionInvoker(null);
		ServiceContextThreadLocal.remove();
	}

	@Test
	void addCompanyRunsWithServiceContextWithoutRequestOrResponse()
		throws Throwable {

		ServiceContext original = new ServiceContext();

		ServiceContextThreadLocal.pushServiceContext(original);

		List<ServiceContext> contexts = new ArrayList<>();
		CompanyCreator creator = new CompanyCreator(
			_companyLocalService(
				(proxy, method, args) -> {
					if (method.getName().equals("addCompany")) {
						contexts.add(ServiceContextThreadLocal.getServiceContext());

						return TestModelProxyUtil.proxy(
							Company.class,
							Map.of("getCompanyId", (long)contexts.size()));
					}

					throw new UnsupportedOperationException(method.getName());
				}));
		ProgressCallback progress = (current, total) -> {};

		BatchResult<Company> result = creator.create(
			2, "webid", "vhost.example.com", "mx.example.com", 0, true,
			progress);

		assertTrue(result.success());
		assertEquals(2, result.count());
		assertEquals(2, contexts.size());

		for (ServiceContext context : contexts) {
			assertNotNull(context);
			assertNotSame(original, context);
			assertNull(context.getRequest());
			assertNull(context.getResponse());
		}

		assertSame(original, ServiceContextThreadLocal.getServiceContext());
	}

	@Test
	void restoresOriginalServiceContextWhenAddCompanyFails() {
		ServiceContext original = new ServiceContext();

		ServiceContextThreadLocal.pushServiceContext(original);

		CompanyCreator creator = new CompanyCreator(
			_companyLocalService(
				(proxy, method, args) -> {
					if (method.getName().equals("addCompany")) {
						throw new PortalException("boom");
					}

					throw new UnsupportedOperationException(method.getName());
				}));
		ProgressCallback progress = (current, total) -> {};

		assertThrows(
			PortalException.class,
			() -> creator.create(
				2, "webid", "vhost.example.com", "mx.example.com", 0, true,
				progress));

		assertSame(original, ServiceContextThreadLocal.getServiceContext());
	}

	private CompanyLocalService _companyLocalService(
		InvocationHandler invocationHandler) {

		return (CompanyLocalService)Proxy.newProxyInstance(
			CompanyLocalService.class.getClassLoader(),
			new Class<?>[] {CompanyLocalService.class}, invocationHandler);
	}

}
