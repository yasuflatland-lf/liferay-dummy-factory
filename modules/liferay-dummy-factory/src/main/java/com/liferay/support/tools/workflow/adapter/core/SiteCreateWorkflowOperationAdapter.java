package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.SiteMembershipType;
import com.liferay.support.tools.service.usecase.SiteCreateUseCase;
import com.liferay.support.tools.service.usecase.SiteItemResult;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.dto.SiteCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class SiteCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	public SiteCreateWorkflowOperationAdapter() {
	}

	SiteCreateWorkflowOperationAdapter(SiteCreateUseCase siteCreateUseCase) {
		_siteCreateUseCase = siteCreateUseCase;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		SiteCreateRequest request = new SiteCreateRequest(
			values.optionalBoolean("active", true),
			new BatchSpec(values.requireCount(), values.requireText("baseName")),
			values.optionalString("description", ""),
			values.optionalBoolean("inheritContent", false),
			values.optionalBoolean("manualMembership", true),
			values.optionalEnum("membershipType", SiteMembershipType::fromString, SiteMembershipType.OPEN),
			values.optionalLong("parentGroupId", 0L),
			values.optionalLong("privateLayoutSetPrototypeId", 0L),
			values.optionalLong("publicLayoutSetPrototypeId", 0L),
			values.optionalLong("siteTemplateId", 0L));

		BatchResult<SiteItemResult> result = _siteCreateUseCase.create(
			workflowExecutionContext.userId(),
			workflowExecutionContext.companyId(), request.batch(),
			request.membershipType(), request.parentGroupId(),
			request.siteTemplateId(), request.manualMembership(),
			request.inheritContent(), request.active(), request.description(),
			request.publicLayoutSetPrototypeId(),
			request.privateLayoutSetPrototypeId(), ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			item -> {
				Map<String, Object> map = new LinkedHashMap<>();

				map.put("groupId", item.groupId());
				map.put("name", item.name());
				map.put("inheritContent", item.inheritContent());
				map.put("parentGroupId", item.parentGroupId());

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
		return "site.create";
	}

	@Reference
	private SiteCreateUseCase _siteCreateUseCase;

}
