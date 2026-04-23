package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.CategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.WorkflowResultNormalizer;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.CategoryCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=category.create",
	service = WorkflowOperationAdapter.class
)
public class CategoryCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	public CategoryCreateWorkflowOperationAdapter() {
	}

	public CategoryCreateWorkflowOperationAdapter(
		CategoryCreator categoryCreator) {
		_categoryCreator = categoryCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		long userId = workflowExecutionContext.userId();

		if (parameters.containsKey("userId")) {
			long overrideUserId = values.optionalLong("userId", 0);

			if (overrideUserId <= 0) {
				throw new IllegalArgumentException("userId must be positive");
			}

			userId = overrideUserId;
		}

		CategoryCreateRequest request = new CategoryCreateRequest(
			userId, values.requirePositiveLong("groupId"),
			values.requirePositiveLong("vocabularyId"),
			new BatchSpec(values.requireCount(), values.requireText("baseName")));

		BatchResult<AssetCategory> result = _categoryCreator.create(
			request.userId(), request.groupId(), request.vocabularyId(),
			request.batch(), ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			category -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("categoryId", category.getCategoryId());
				item.put("groupId", category.getGroupId());
				item.put("vocabularyId", category.getVocabularyId());
				item.put("name", category.getName());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "category.create";
	}

	@Reference
	private CategoryCreator _categoryCreator;

}
