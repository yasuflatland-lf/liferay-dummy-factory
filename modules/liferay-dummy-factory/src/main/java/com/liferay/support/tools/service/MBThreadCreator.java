package com.liferay.support.tools.service;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.service.MBMessageLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MBThreadCreator.class)
public class MBThreadCreator {

	public BatchResult<MBMessage> create(
			long userId, long groupId, long categoryId, BatchSpec batchSpec,
			String body, String format, ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();

		String userName = _userLocalService.getUser(userId).getFullName();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		List<MBMessage> messages = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String subject = BatchNaming.resolve(
				batchSpec.baseName(), count, i, " ");

			messages.add(
				BatchTransaction.run(
					() -> _mbMessageLocalService.addMessage(
						null, userId, userName, groupId, categoryId, 0L, 0L,
						subject, body, format, Collections.emptyList(), false,
						0.0, false, serviceContext)));

			progress.onProgress(i + 1, count);
		}

		return BatchResult.success(count, messages, 0);
	}

	@Reference
	private MBMessageLocalService _mbMessageLocalService;

	@Reference
	private UserLocalService _userLocalService;

}
