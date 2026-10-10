package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.liferay.support.tools.workflow.jaxrs.WorkflowOpenAPIDocumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowOpenAPIIdentityTest {

	@Test
	void operationSchemasExcludeExecutionIdentityParameters() {
		List<WorkflowFunctionDescriptor> descriptors = new ArrayList<>(
			WorkflowFunctionDescriptors.descriptors().values());
		Map<String, Object> paths = _map(
			WorkflowOpenAPIDocumentBuilder.build(Map.of(), descriptors), "paths");

		for (WorkflowFunctionDescriptor descriptor : descriptors) {
			Map<String, Object> post = _map(
				_map(paths, "/operations/" + descriptor.operation()), "post");
			Map<String, Object> schema = _map(
				_map(_map(_map(post, "requestBody"), "content"), "application/json"),
				"schema");
			Map<String, Object> properties = _map(schema, "properties");

			assertFalse(properties.containsKey("userId"), descriptor.operation());
			assertFalse(properties.containsKey("companyId"), descriptor.operation());
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> _map(Map<String, Object> map, String key) {
		return (Map<String, Object>)map.get(key);
	}

}
