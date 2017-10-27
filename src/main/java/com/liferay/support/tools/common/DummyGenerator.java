package com.liferay.support.tools.common;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;

import javax.portlet.ActionRequest;

public abstract class DummyGenerator<T extends ParamContext> {

	/**
	 * Get Context
	 *
	 * @param request
	 * @return
	 */
	protected abstract T getContext(ActionRequest request) throws Exception;

	/**
	 * Create Dummy data
	 *
	 * @param request
	 * @throws Exception
	 */
	public void create(ActionRequest request) throws Exception {

		T paramContext = getContext(request);

        if(!validate(paramContext)) {
            throw new Exception("Validation Error");
        }

		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		ServiceContext serviceContext = ServiceContextFactory.getInstance(Group.class.getName(), request);

		paramContext.setThemeDisplay(themeDisplay);
		paramContext.setServiceContext(serviceContext);

		exec(request, paramContext);
	}

	/**
	 * Validation
	 *
	 * @param paramContext
	 * @return boolean
     */
	protected boolean validate(T paramContext) {
		return true;
	}

	/**
	 * Exec data
	 *
	 * @param paramContext
	 */
	protected abstract void exec(ActionRequest request, T paramContext) throws Exception;
}