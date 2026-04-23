package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.VocabularyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.WorkflowResultNormalizer;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.VocabularyCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=vocabulary.create",
	service = WorkflowOperationAdapter.class
)
public class VocabularyCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	public VocabularyCreateWorkflowOperationAdapter() {
	}

	public VocabularyCreateWorkflowOperationAdapter(
		VocabularyCreator vocabularyCreator) {

		_vocabularyCreator = vocabularyCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		long userId = _resolveUserIdOverride(
			values, workflowExecutionContext, parameters);
		VocabularyCreateRequest request = new VocabularyCreateRequest(
			userId, values.requirePositiveLong("groupId"),
			new BatchSpec(values.requireCount(), values.requireText("baseName")));

		BatchResult<AssetVocabulary> result = _vocabularyCreator.create(
			request.userId(), request.groupId(), request.batch(),
			ProgressCallback.NOOP);

		return WorkflowResultNormalizer.normalize(
			result,
			vocabulary -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("vocabularyId", vocabulary.getVocabularyId());
				item.put("groupId", vocabulary.getGroupId());
				item.put("name", vocabulary.getName());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "vocabulary.create";
	}

	@Reference
	private VocabularyCreator _vocabularyCreator;

	private static long _resolveUserIdOverride(
		WorkflowParameterValues values,
		WorkflowExecutionContext workflowExecutionContext,
		Map<String, Object> parameters) {

		if (parameters == null) {
			return workflowExecutionContext.userId();
		}

		Object rawUserId = parameters.get("userId");

		if (rawUserId == null) {
			return workflowExecutionContext.userId();
		}

		if ((rawUserId instanceof String string) && string.isBlank()) {
			return workflowExecutionContext.userId();
		}

		long userId = values.optionalLong(
			"userId", workflowExecutionContext.userId());

		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be greater than 0");
		}

		return userId;
	}

}
