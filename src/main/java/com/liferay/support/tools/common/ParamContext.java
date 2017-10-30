package com.liferay.support.tools.common;

import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.theme.ThemeDisplay;

public abstract class ParamContext {

	protected ThemeDisplay themeDisplay;
	protected ServiceContext serviceContext;

	public ThemeDisplay getThemeDisplay() {
		return themeDisplay;
	}

	public void setThemeDisplay(ThemeDisplay themeDisplay) {
		this.themeDisplay = themeDisplay;
	}

	public ServiceContext getServiceContext() {
		return serviceContext;
	}

	public void setServiceContext(ServiceContext serviceContext) {
		this.serviceContext = serviceContext;
	}

}
