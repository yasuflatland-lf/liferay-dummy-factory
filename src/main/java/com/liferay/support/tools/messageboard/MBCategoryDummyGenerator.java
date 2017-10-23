package com.liferay.support.tools.messageboard;

import com.liferay.message.boards.kernel.service.MBCategoryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.CommonUtil;
import com.liferay.support.tools.utils.ProgressManager;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Message Board Categories Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = MBCategoryDummyGenerator.class)
public class MBCategoryDummyGenerator extends DummyGenerator<MBContext> {

	public MBCategoryDummyGenerator() {}
	
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

		for (long groupId : paramContext.getGroupIds()) {
			System.out.println("Starting to create " + paramContext.getNumberOfMB() + " categories for site id <"
					+ _groupLocalService.getGroup(groupId).getDescriptiveName() + ">");

			for (long i = 1; i <= paramContext.getNumberOfMB(); i++) {
				// Update progress
				progressManager.trackProgress(progressCount,
						paramContext.getNumberOfMB() * paramContext.getGroupIds().length);

				// Create Site Name
				StringBundler actualName = new StringBundler(2);
				actualName.append(paramContext.getCategoryName());

				// Add number more then one site
				if (1 < paramContext.getNumberOfMB()) {
					actualName.append(i);
				}

				try {

					// Generate category
					_MBCategoryLocalService.addCategory(
							paramContext.getServiceContext().getUserId(), // userId
							paramContext.getParentCategoryId(), // parentCategoryId
							actualName.toString(), // name
							paramContext.getDescription(), // description,
							paramContext.getServiceContext());

				} catch (Throwable e) {
					// Finish progress
					progressManager.finish();
					throw e;
				}
				progressCount++;
			}
		}

		// Finish progress
		progressManager.finish();

		System.out.println("Finished creating " + paramContext.getNumberOfMB() + " categories");
	}

	@Reference
	private CommonUtil _commonUtil;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private MBCategoryLocalService _MBCategoryLocalService;;
}
