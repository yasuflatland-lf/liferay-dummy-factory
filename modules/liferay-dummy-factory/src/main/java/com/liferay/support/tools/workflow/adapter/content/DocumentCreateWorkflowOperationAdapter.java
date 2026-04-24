package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.support.tools.service.AssetTagNames;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.DocumentBatchSpec;
import com.liferay.support.tools.service.DocumentCreator;
import com.liferay.support.tools.workflow.adapter.core.WorkflowResultNormalizer;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=document.create",
	service = WorkflowOperationAdapter.class
)
public class DocumentCreateWorkflowOperationAdapter
	extends AbstractCreatorWorkflowOperationAdapter {

	public DocumentCreateWorkflowOperationAdapter() {
	}

	DocumentCreateWorkflowOperationAdapter(DocumentCreator documentCreator) {
		_documentCreator = documentCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		DocumentCreateRequest request = DocumentCreateRequest.from(
			workflowExecutionContext, parameters(parameters));

		DocumentBatchSpec spec = new DocumentBatchSpec(
			request.batchSpec(), request.groupId(), request.folderId(),
			request.description(), request.uploadedFiles(),
			AssetTagNames.EMPTY);

		BatchResult<FileEntry> result = _documentCreator.create(
			request.userId(), spec, progressCallback());

		return WorkflowResultNormalizer.normalize(
			result,
			fileEntry -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("fileEntryId", fileEntry.getFileEntryId());
				item.put("title", fileEntry.getTitle());

				return item;
			});
	}

	@Override
	public String operationName() {
		return "document.create";
	}

	@Reference
	private DocumentCreator _documentCreator;

}
