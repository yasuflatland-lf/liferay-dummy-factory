package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.portal.kernel.model.Role;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.RoleCreator;
import com.liferay.support.tools.service.RoleType;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.dto.RoleCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class RoleCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		RoleCreateRequest request = new RoleCreateRequest(
			new BatchSpec(values.requireCount(), values.requireText("baseName")),
			values.optionalString("description", ""),
			values.optionalEnum("roleType", RoleType::fromString, RoleType.REGULAR));

		BatchResult<Role> result = _roleCreator.create(
			workflowExecutionContext.userId(), request.batch(),
			request.roleType(), request.description(), ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			role -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("name", role.getName());
				item.put("roleId", role.getRoleId());
				item.put("type", role.getType());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "role.create";
	}

	@Reference
	private RoleCreator _roleCreator;
}
