package com.liferay.support.tools.workflow.adapter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.liferay.portal.kernel.model.Company;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.CompanyCreator;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CompanyCreateWorkflowOperationAdapterTest {

	@Test
	void executeSetsSkippedForPartialResults() throws Throwable {
		CompanyCreateWorkflowOperationAdapter adapter =
			new CompanyCreateWorkflowOperationAdapter(
				new CompanyCreator() {

					@Override
					public BatchResult<Company> create(
						int count, String webId, String virtualHostname,
						String mx, int maxUsers, boolean active,
						com.liferay.support.tools.utils.ProgressCallback progress) {

						List<Company> companies = List.of(
							_company(101L, "company-1"));

						return BatchResult.failure(
							count, companies, count - companies.size(),
							"Only 1 of 2 companies were created.");
					}

				});

		WorkflowStepResult result = adapter.execute(
			new WorkflowExecutionContext(1L),
			Map.of(
				"active", true, "count", 2, "maxUsers", 100,
				"mx", "mx.example.com", "virtualHostname", "vhost",
				"webId", "company"));

		assertEquals(2, result.requested());
		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertFalse(result.success());
		assertEquals(
			"Only 1 of 2 companies were created.", result.error());
		assertEquals(
			Map.of("companyId", 101L, "webId", "company-1"),
			result.items().get(0));
		assertEquals("company.create", adapter.operationName());
	}

	private static Company _company(long companyId, String webId) {
		return TestModelProxyUtil.proxy(
			Company.class,
			Map.of("getCompanyId", companyId, "getWebId", webId));
	}
}
