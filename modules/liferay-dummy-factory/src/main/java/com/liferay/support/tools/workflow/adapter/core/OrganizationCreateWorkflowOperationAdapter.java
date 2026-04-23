package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.portal.kernel.model.Organization;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.OrganizationCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.dto.OrganizationCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class OrganizationCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		OrganizationCreateRequest request = new OrganizationCreateRequest(
			new BatchSpec(values.requireCount(), values.requireText("baseName")),
			values.optionalLong("parentOrganizationId", 0L),
			values.optionalBoolean("site", false));

		BatchResult<Organization> result = _organizationCreator.create(
			workflowExecutionContext.userId(), request.batch(),
			request.parentOrganizationId(), request.site(),
			ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			organization -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("name", organization.getName());
				item.put(
					"organizationId", organization.getOrganizationId());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "organization.create";
	}

	@Reference
	private OrganizationCreator _organizationCreator;
}
