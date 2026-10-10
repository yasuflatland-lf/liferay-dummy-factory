package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.liferay.support.tools.workflow.jaxrs.WorkflowOpenAPIDocumentBuilder;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowOperationIdCoverageTest {

	@Test
	void everyDescriptorHasAnOperationId() {
		for (String operation : WorkflowFunctionDescriptors.descriptors().keySet()) {
			assertNotNull(WorkflowOpenAPIDocumentBuilder.operationIdOf(operation), operation);
		}
	}

	@Test
	void descriptorsExcludeExecutionIdentityParameters() {
		for (WorkflowFunctionDescriptor descriptor :
				WorkflowFunctionDescriptors.descriptors().values()) {

			List<String> parameterNames = descriptor.parameters().stream(
			).map(
				WorkflowFunctionParameter::name
			).toList();

			assertFalse(parameterNames.contains("userId"), descriptor.operation());
			assertFalse(parameterNames.contains("companyId"), descriptor.operation());
		}
	}

	@Test
	void operationIdsAreUnique() {
		List<String> operationIds = WorkflowFunctionDescriptors.descriptors().keySet().stream(
		).map(
			WorkflowOpenAPIDocumentBuilder::operationIdOf
		).toList();

		assertEquals(operationIds.size(), new HashSet<>(operationIds).size());
	}

}
