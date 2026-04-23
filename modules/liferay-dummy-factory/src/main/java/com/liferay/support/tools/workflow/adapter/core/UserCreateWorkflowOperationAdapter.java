package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.UserBatchSpec;
import com.liferay.support.tools.service.usecase.UserCreateUseCase;
import com.liferay.support.tools.service.usecase.UserItemResult;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.dto.UserCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class UserCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	public UserCreateWorkflowOperationAdapter() {
	}

	UserCreateWorkflowOperationAdapter(UserCreateUseCase userCreateUseCase) {
		_userCreateUseCase = userCreateUseCase;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		UserCreateRequest request = new UserCreateRequest(
			new BatchSpec(values.requireCount(), values.requireText("baseName")),
			values.optionalString("emailDomain", null),
			values.optionalBoolean("fakerEnable", false),
			values.optionalBoolean("generatePersonalSiteLayouts", false),
			values.optionalPositiveLongArray("groupIds"),
			values.optionalString("jobTitle", ""),
			values.optionalString("locale", "en_US"),
			values.optionalBoolean("male", true),
			values.optionalPositiveLongArray("orgRoleIds"),
			values.optionalPositiveLongArray("organizationIds"),
			values.optionalString("password", "test"),
			values.optionalLong("privateLayoutSetPrototypeId", 0L),
			values.optionalLong("publicLayoutSetPrototypeId", 0L),
			values.optionalPositiveLongArray("roleIds"),
			values.optionalPositiveLongArray("siteRoleIds"),
			values.optionalPositiveLongArray("userGroupIds"));

		UserBatchSpec userBatchSpec = request.toUserBatchSpec();

		BatchResult<UserItemResult> result = _userCreateUseCase.create(
			workflowExecutionContext.userId(),
			workflowExecutionContext.companyId(), userBatchSpec,
			ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			item -> {
				Map<String, Object> map = new LinkedHashMap<>();

				map.put("emailAddress", item.emailAddress());
				map.put("screenName", item.screenName());
				map.put("userId", item.userId());

				if (item.groupId() != 0L) {
					map.put("groupId", item.groupId());
				}

				if (item.publicLayoutSetPrototypeUuid() != null) {
					map.put(
						"publicLayoutSetPrototypeUuid",
						item.publicLayoutSetPrototypeUuid());
				}

				if (item.privateLayoutSetPrototypeUuid() != null) {
					map.put(
						"privateLayoutSetPrototypeUuid",
						item.privateLayoutSetPrototypeUuid());
				}

				return map;
			});
	}

	@Override
	public String operationName() {
		return "user.create";
	}

	@Reference
	private UserCreateUseCase _userCreateUseCase;

}
