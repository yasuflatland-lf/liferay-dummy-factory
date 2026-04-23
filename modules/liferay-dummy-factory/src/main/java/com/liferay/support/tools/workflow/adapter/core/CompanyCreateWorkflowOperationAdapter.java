package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.portal.kernel.model.Company;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.CompanyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class CompanyCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	public CompanyCreateWorkflowOperationAdapter() {
	}

	CompanyCreateWorkflowOperationAdapter(CompanyCreator companyCreator) {
		_companyCreator = companyCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		int requested = values.requireCount();

		BatchResult<Company> result = _companyCreator.create(
			requested, values.requireText("webId"),
			values.requireText("virtualHostname"), values.requireText("mx"),
			values.optionalInt("maxUsers", 0),
			values.optionalBoolean("active", true), ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			company -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("companyId", company.getCompanyId());
				item.put("webId", company.getWebId());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "company.create";
	}

	@Reference
	private CompanyCreator _companyCreator;

}
