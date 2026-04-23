package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.service.WebContentCreator;
import com.liferay.support.tools.service.WebContentPerSiteResult;
import com.liferay.support.tools.workflow.adapter.core.WorkflowResultNormalizer;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=webContent.create",
	service = WorkflowOperationAdapter.class
)
public class WebContentCreateWorkflowOperationAdapter
	extends AbstractCreatorWorkflowOperationAdapter {

	public WebContentCreateWorkflowOperationAdapter() {
	}

	WebContentCreateWorkflowOperationAdapter(
		Portal portal, WebContentCreator webContentCreator) {

		_portal = portal;
		_webContentCreator = webContentCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WebContentCreateRequest request = WebContentCreateRequest.from(
			workflowExecutionContext, parameters(parameters), _portal);

		BatchResult<WebContentPerSiteResult> result = _webContentCreator.create(
			request.userId(), request.batchSpec(), progressCallback());

		return WorkflowResultNormalizer.normalize(
			result,
			perSite -> {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("groupId", perSite.groupId());
				item.put("siteName", perSite.siteName());
				item.put("created", perSite.created());
				item.put("failed", perSite.failed());

				if (perSite.error() != null) {
					item.put("error", perSite.error());
				}

				return item;
			});
	}

	@Override
	public String operationName() {
		return "webContent.create";
	}

	@Reference
	private Portal _portal;

	@Reference
	private WebContentCreator _webContentCreator;

}
