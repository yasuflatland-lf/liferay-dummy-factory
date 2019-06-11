package com.liferay.support.tools.messageboard;

import com.liferay.message.boards.constants.MBCategoryConstants;
import com.liferay.message.boards.model.MBCategory;
import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.model.MBThread;
import com.liferay.message.boards.service.MBCategoryLocalService;
import com.liferay.message.boards.service.MBMessageLocalService;
import com.liferay.message.boards.service.MBThreadLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.CommonUtil;

import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Message Board resources
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=" + LDFPortletKeys.CMD_MB_LIST
	},
	service = MVCResourceCommand.class
)
public class MBMVCResourceCommand extends BaseMVCResourceCommand {

	/**
	 * Commands
	 */
	static public final String CMD_THREAD_LIST = "threadlist";
	static public final String CMD_CATEGORY_LIST = "categorylist";
	
	@Override
	protected void doServeResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws Exception {
		String cmd = ParamUtil.getString(resourceRequest, Constants.CMD);
		String serializedJson = "";
		
		if(cmd.equals(CMD_THREAD_LIST)) {
			serializedJson = getThreadList(resourceRequest,resourceResponse );
			
		} else if (cmd.equals(CMD_CATEGORY_LIST)) {
			serializedJson = getCategoryList(resourceRequest,resourceResponse );
			
		} else {
			_log.error("Unknown command is passed <" + cmd + ">");
		}
		
		HttpServletResponse response = _portal.getHttpServletResponse(
				resourceResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, serializedJson);		
	}
	
	/**
	 * Get Category list
	 * 
	 * @param resourceRequest
	 * @param resourceResponse
	 * @return
	 * @throws PortalException 
	 */
	private String getCategoryList(ResourceRequest resourceRequest, ResourceResponse resourceResponse) throws PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
		 
		String[] groupsStrIds = ParamUtil.getStringValues(resourceRequest, "groupIds",
				new String[] { String.valueOf(themeDisplay.getScopeGroupId()) });
		long[] groupIds = CommonUtil.convertStringToLongArray(groupsStrIds);		
		long siteGroupId = groupIds[0];

		if(_log.isDebugEnabled()) {
			_log.debug("SiteGroup Id <" + String.valueOf(siteGroupId) + ">");
		}
		
		List<MBCategory> categories = _mbCategoryLocalService.getCategories(
				siteGroupId, 
				MBCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, 
				WorkflowConstants.STATUS_APPROVED, 
				QueryUtil.ALL_POS, 
				QueryUtil.ALL_POS);
		
		for(MBCategory category : categories ) {
			JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();
			
			if(_log.isDebugEnabled()) {
				_log.debug("Category name <" + category.getName() + ">");
				_log.debug(category.toString());
				_log.debug("----------");
			}
			
			curUserJSONObject.put("categoryId" , category.getCategoryId());
			curUserJSONObject.put("categoryName" , category.getName());
			
			jsonArray.put(curUserJSONObject);
		}
		
		return jsonArray.toJSONString();
	}

	/**
	 * Get Thread list
	 * 
	 * Depending on the passed site groupd id, fetch all pages in the site and return JSON object list.
	 * 
	 * @param resourceRequest
	 * @param resourceResponse
	 * @return
	 * @throws PortalException 
	 */
	protected String getThreadList(ResourceRequest resourceRequest, ResourceResponse resourceResponse) 
			throws PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
		long siteGroupId = ParamUtil.getLong(resourceRequest, "siteGroupId", themeDisplay.getSiteGroupId());

		if(_log.isDebugEnabled()) {
			_log.debug("SiteGroup Id <" + String.valueOf(siteGroupId) + ">");
		}

		List<MBThread> threads = _mbThreadLocalService.getThreads(
				siteGroupId, 
				MBCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, 
				WorkflowConstants.STATUS_APPROVED, 
				QueryUtil.ALL_POS, 
				QueryUtil.ALL_POS);
		
		for(MBThread thread : threads ) {
			JSONObject curUserJSONObject = JSONFactoryUtil.createJSONObject();
			
			MBMessage message = _mbMessageLocalService.getMessage(thread.getRootMessageId());
			
			if(_log.isDebugEnabled()) {
				_log.debug("Root message <" + message.getSubject() + ">");
				_log.debug(thread.toString());
				_log.debug("----------");
			}
			
			curUserJSONObject.put("rootMessageSubject" , message.getSubject());
			curUserJSONObject.put("threadId" , thread.getThreadId());
			curUserJSONObject.put("rootMessageId" , thread.getRootMessageId());
			
			jsonArray.put(curUserJSONObject);
		}
		
		return jsonArray.toJSONString();
	}
	
	@Reference
	private MBThreadLocalService _mbThreadLocalService;		
	
	@Reference
	private MBCategoryLocalService _mbCategoryLocalService;

	@Reference
	private MBMessageLocalService _mbMessageLocalService;
	
	@Reference
	private Portal _portal;	
	
	private static final Log _log = LogFactoryUtil.getLog(
			MBMVCResourceCommand.class);		
}
