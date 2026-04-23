package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBCategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.WorkflowResultNormalizer;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=mbCategory.create",
	service = WorkflowOperationAdapter.class
)
public class MBCategoryCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	public static final String OPERATION = "mbCategory.create";

	public MBCategoryCreateWorkflowOperationAdapter() {
	}

	MBCategoryCreateWorkflowOperationAdapter(
		MBCategoryCreator mbCategoryCreator) {

		_mbCategoryCreator = mbCategoryCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		MBCategoryCreateRequest request = new MBCategoryCreateRequest(
			_effectiveUserId(values, workflowExecutionContext),
			values.requirePositiveLong("groupId"), _batchSpec(values),
			values.requireText("description"));

		BatchResult<MBCategory> result = _mbCategoryCreator.create(
			request.userId(), request.groupId(), request.batch(),
			request.description(), ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			category -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("categoryId", category.getCategoryId());
				item.put("groupId", category.getGroupId());
				item.put("name", category.getName());

				return item;
			});
	}

	@Override
	public String operationName() {
		return OPERATION;
	}

	private static BatchSpec _batchSpec(WorkflowParameterValues values) {
		return new BatchSpec(values.requireCount(), values.requireText("baseName"));
	}

	private static long _effectiveUserId(
		WorkflowParameterValues values,
		WorkflowExecutionContext workflowExecutionContext) {

		long userId = values.optionalLong("userId", workflowExecutionContext.userId());

		if (userId <= 0) {
			throw new IllegalArgumentException("userId is required");
		}

		return userId;
	}

	@Reference
	private MBCategoryCreator _mbCategoryCreator;

}
