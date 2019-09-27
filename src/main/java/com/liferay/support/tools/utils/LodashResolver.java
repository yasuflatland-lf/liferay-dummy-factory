package com.liferay.support.tools.utils;

import com.liferay.frontend.js.loader.modules.extender.npm.JSPackage;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;

import javax.portlet.RenderRequest;

public class LodashResolver {

	static public void exec(RenderRequest renderRequest, NPMResolver npmResolver) {
		JSPackage jsPackage = npmResolver.getDependencyJSPackage("lodash");
		renderRequest.setAttribute("bootstrapRequire", jsPackage.getResolvedId());
	}
}
