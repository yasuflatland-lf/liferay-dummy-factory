package com.liferay.support.tools.workflow.jaxrs;

import jakarta.ws.rs.core.Application;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"osgi.jaxrs.application.base=/ldf-workflow",
		"osgi.jaxrs.name=ldf-workflow"
	},
	service = Application.class
)
public class WorkflowApplication extends Application {
}
