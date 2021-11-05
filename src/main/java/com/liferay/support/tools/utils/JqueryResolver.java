package com.liferay.support.tools.utils;

import com.liferay.frontend.js.loader.modules.extender.npm.JSPackage;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;

import javax.portlet.RenderRequest;

public class JqueryResolver {
    static public void exec(RenderRequest renderRequest, NPMResolver npmResolver) {
        JSPackage jsPackage = npmResolver.getDependencyJSPackage("jquery");
        renderRequest.setAttribute("jqueryResolver", jsPackage.getResolvedId());
    }
}
