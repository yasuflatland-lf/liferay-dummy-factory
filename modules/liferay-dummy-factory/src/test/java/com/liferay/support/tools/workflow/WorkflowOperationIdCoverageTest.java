package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void operationIdsAreUnique() {
		List<String> operationIds = WorkflowFunctionDescriptors.descriptors().keySet().stream(
		).map(
			WorkflowOpenAPIDocumentBuilder::operationIdOf
		).toList();

		assertEquals(operationIds.size(), new HashSet<>(operationIds).size());
	}

}
