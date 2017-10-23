package com.liferay.support.tools.messageboard;

import com.liferay.message.boards.kernel.model.MBMessageConstants;
import com.liferay.message.boards.kernel.model.MBThread;
import com.liferay.message.boards.kernel.service.MBMessageService;
import com.liferay.message.boards.kernel.service.MBThreadLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.CommonUtil;
import com.liferay.support.tools.utils.ProgressManager;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Message Board Threads Reply Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = MBReplyDummyGenerator.class)
public class MBReplyDummyGenerator extends DummyGenerator<MBContext> {

	@Override
	protected MBContext getContext(ActionRequest request) {

		return new MBContext(request);
	}

	@Override
	protected void exec(ActionRequest request, MBContext paramContext) throws PortalException {

		// Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		long progressCount = 0;
		List<ObjectValuePair<String, InputStream>> inputStreamOVPs = Collections.emptyList();
		
		for (long i = 1; i <= paramContext.getNumberOfMB(); i++) {
			// Update progress
			progressManager.trackProgress(progressCount,
					paramContext.getNumberOfMB() * paramContext.getGroupIds().length);

			// Create Site Name
			StringBundler actualSubject = new StringBundler(2);
			actualSubject.append(paramContext.getSubject());

			// Add number more then one site
			if (1 < paramContext.getNumberOfMB()) {
				actualSubject.append(i);
			}

			try {
				
				// Get Thread
				MBThread thread = _mbThreadLocalService.getThread(paramContext.getThreadId());
				
				// Add Response to the message in the thread
				_mbMessageService.addMessage(
						thread.getRootMessageId(), // parentMessageId
						actualSubject.toString(), // subject,
						paramContext.getBody(), // body,
						MBMessageConstants.DEFAULT_FORMAT, // format,
						inputStreamOVPs, // inputStreamOVPs,
						paramContext.isAnonymous(), // anonymous,
						paramContext.getPriority(), // priority,
						paramContext.isAllowPingbacks(), // allowPingbacks,
						paramContext.getServiceContext());

			} catch (Throwable e) {
				// Finish progress
				progressManager.finish();
				throw e;
			}
			progressCount++;
		}

		// Finish progress
		progressManager.finish();
		
		System.out.println("Finished creating " + paramContext.getNumberOfMB() + " threads");

	}

	@Reference
	private CommonUtil _commonUtil;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private MBMessageService _mbMessageService;
	
	@Reference
	private MBThreadLocalService _mbThreadLocalService;
}
