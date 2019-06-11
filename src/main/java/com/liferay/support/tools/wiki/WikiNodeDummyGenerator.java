package com.liferay.support.tools.wiki;

import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;
import com.liferay.wiki.service.WikiNodeLocalService;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = WikiNodeDummyGenerator.class)
public class WikiNodeDummyGenerator extends DummyGenerator<WikiContext> {

	@Override
	protected WikiContext getContext(ActionRequest request) throws Exception {
		return new WikiContext(request);
	}

	@Override
	protected void exec(ActionRequest request, WikiContext paramContext) throws Exception {
		// Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		System.out.println("Starting to create " + paramContext.getNumberOfnodes() + " nodes");

		// Set workflow as approved
		paramContext.getServiceContext().setWorkflowAction(WorkflowConstants.STATUS_APPROVED);
		
		// Replace service context's group ID to paramaeter's group ID
		paramContext.getServiceContext().setScopeGroupId(paramContext.getGroupId());
		
		for (long i = 1; i <= paramContext.getNumberOfnodes(); i++) {
			// Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfnodes());

			// Create page name
			StringBundler name = new StringBundler(2);
			name.append(paramContext.getBaseNodeName());

			// Add number more then one Page
			if (1 < paramContext.getNumberOfnodes()) {
				name.append(i);
			}

			try {
				_wikiNodeLocalService.addNode(
						paramContext.getServiceContext().getUserId(),
						name.toString(), 
						name.toString(), 
						paramContext.getServiceContext()); // serviceContext

			} catch (Exception e) {
				// Finish progress
				progressManager.finish();
				throw e;
			}

		}

		// Finish progress
		progressManager.finish();

		System.out.println("Finished creating " + paramContext.getNumberOfnodes() + " nodes");
	}

	@Reference
	private WikiNodeLocalService _wikiNodeLocalService;

	@Reference
	private GroupLocalService _groupLocalService;
}
