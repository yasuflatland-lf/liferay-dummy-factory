package com.liferay.support.tools.service;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.model.MBThread;
import com.liferay.message.boards.service.MBMessageLocalService;
import com.liferay.message.boards.service.MBThreadLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.utils.RandomizeContentGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MBReplyCreator.class)
public class MBReplyCreator {

	public BatchResult<MBMessage> create(
			long userId, MBReplyBatchSpec spec, ProgressCallback progress)
		throws Throwable {

		BatchSpec batchSpec = spec.batch();
		int count = batchSpec.count();

		MBThread thread = _mbThreadLocalService.getMBThread(spec.threadId());

		long groupId = thread.getGroupId();
		long categoryId = thread.getCategoryId();
		long rootMessageId = thread.getRootMessageId();

		String userName = _userLocalService.getUser(userId).getFullName();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		if (!spec.tags().isEmpty()) {
			serviceContext.setAssetTagNames(spec.tags().toArray());
		}

		List<MBMessage> replies = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			final int idx = i;

			String subject =
				"RE: " + BatchNaming.resolve(batchSpec.baseName(), count, idx);

			final String actualBody = spec.fakerEnable()
				? _randomizeContentGenerator.generateRandomContents(
					spec.locale(), 3, 0, "")
				: spec.body();

			replies.add(
				BatchTransaction.run(
					() -> _mbMessageLocalService.addMessage(
						null, userId, userName, groupId, categoryId,
						spec.threadId(), rootMessageId, subject, actualBody,
						spec.format(), Collections.emptyList(), false, 0.0,
						false, serviceContext)));

			progress.onProgress(idx + 1, count);
		}

		return BatchResult.success(count, replies, 0);
	}

	@Reference
	private MBMessageLocalService _mbMessageLocalService;

	@Reference
	private MBThreadLocalService _mbThreadLocalService;

	@Reference
	private RandomizeContentGenerator _randomizeContentGenerator;

	@Reference
	private UserLocalService _userLocalService;

}
