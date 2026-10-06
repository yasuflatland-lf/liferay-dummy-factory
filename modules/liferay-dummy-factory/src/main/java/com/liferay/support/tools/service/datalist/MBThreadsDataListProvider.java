package com.liferay.support.tools.service.datalist;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.model.MBThread;
import com.liferay.message.boards.service.MBCategoryLocalService;
import com.liferay.message.boards.service.MBMessageLocalService;
import com.liferay.message.boards.service.MBThreadLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class MBThreadsDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		return JSONFactoryUtil.createJSONArray();
	}

	@Override
	public JSONArray getOptions(
			long companyId, String type,
			HttpServletRequest httpServletRequest)
		throws Exception {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long groupId = ParamUtil.getLong(httpServletRequest, "groupId", 0);

		if (groupId > 0) {
			_addAllGroupThreads(jsonArray, groupId);

			return jsonArray;
		}

		List<Group> groups = _groupLocalService.getGroups(
			companyId, GroupConstants.DEFAULT_PARENT_GROUP_ID, true);

		for (Group group : groups) {
			_addAllGroupThreads(jsonArray, group.getGroupId());
		}

		return jsonArray;
	}

	private void _addAllGroupThreads(JSONArray jsonArray, long groupId)
		throws Exception {

		_addThreads(
			jsonArray,
			_mbThreadLocalService.getThreads(
				groupId, 0L, WorkflowConstants.STATUS_ANY, QueryUtil.ALL_POS,
				QueryUtil.ALL_POS));

		List<MBCategory> categories = _mbCategoryLocalService.getCategories(
			groupId);

		for (MBCategory category : categories) {
			_addThreads(
				jsonArray,
				_mbThreadLocalService.getThreads(
					groupId, category.getCategoryId(),
					WorkflowConstants.STATUS_ANY, QueryUtil.ALL_POS,
					QueryUtil.ALL_POS));
		}
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"mb-threads"};
	}

	private void _addThreads(JSONArray jsonArray, List<MBThread> threads)
		throws Exception {

		for (MBThread thread : threads) {
			MBMessage rootMessage = _mbMessageLocalService.getMessage(
				thread.getRootMessageId());

			jsonArray.put(
				createOption(rootMessage.getSubject(), thread.getThreadId()));
		}
	}

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private MBCategoryLocalService _mbCategoryLocalService;

	@Reference
	private MBMessageLocalService _mbMessageLocalService;

	@Reference
	private MBThreadLocalService _mbThreadLocalService;

}
