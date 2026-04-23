package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;

class WorkflowFunctionFactoryTest {

	@Test
	void createSupportsUnknownSpiOperationWithGenericDescriptor()
		throws Exception {

		WorkflowOperationAdapter adapter = new WorkflowOperationAdapter() {

			@Override
			public com.liferay.support.tools.workflow.spi.WorkflowStepResult execute(
				WorkflowExecutionContext workflowExecutionContext,
				Map<String, Object> parameters) {

				return new com.liferay.support.tools.workflow.spi.WorkflowStepResult(
					true, 1, 1, 0, List.of(Map.of("ok", true)), null);
			}

			@Override
			public String operationName() {
				return "custom.dynamic.operation";
			}
		};

		WorkflowFunction workflowFunction = new WorkflowFunctionFactory().create(
			adapter);

		assertEquals("custom.dynamic.operation", workflowFunction.operation());
		assertNotNull(workflowFunction.executor());
		assertNotNull(
			((DefaultWorkflowFunction)workflowFunction).descriptor());

		WorkflowStepResult result = workflowFunction.executor().execute(
			new WorkflowStepExecutionRequest(
				"w-1", "s-1", "custom.dynamic.operation", "idem-1", Map.of(),
				new DefaultWorkflowExecutionContext(
					Map.of(), 1001L, 2002L)));

		assertEquals(true, result.success());
		assertEquals(1, result.count());
	}

	@Test
	void createUsesExecutionContextIdentityInsteadOfInputOverrides()
		throws Exception {

		AtomicReference<WorkflowExecutionContext> capturedContext =
			new AtomicReference<>();

		WorkflowOperationAdapter adapter = new WorkflowOperationAdapter() {

			@Override
			public com.liferay.support.tools.workflow.spi.WorkflowStepResult execute(
				WorkflowExecutionContext workflowExecutionContext,
				Map<String, Object> parameters) {

				capturedContext.set(workflowExecutionContext);

				return new com.liferay.support.tools.workflow.spi.WorkflowStepResult(
					true, 1, 1, 0, List.of(Map.of("ok", true)), null);
			}

			@Override
			public String operationName() {
				return "custom.identity.operation";
			}
		};

		WorkflowFunction workflowFunction = new WorkflowFunctionFactory().create(
			adapter);

		workflowFunction.executor().execute(
			new WorkflowStepExecutionRequest(
				"w-1", "s-1", "custom.identity.operation", "idem-1", Map.of(),
				new DefaultWorkflowExecutionContext(
					Map.of("userId", 999L, "companyId", 888L), 1001L, 2002L)));

		assertEquals(1001L, capturedContext.get().userId());
		assertEquals(2002L, capturedContext.get().companyId());
		assertTrue(capturedContext.get().userId() != 999L);
		assertTrue(capturedContext.get().companyId() != 888L);
	}

	@Test
	void createPreservesAdapterStepData() throws Exception {
		WorkflowOperationAdapter adapter = new WorkflowOperationAdapter() {

			@Override
			public com.liferay.support.tools.workflow.spi.WorkflowStepResult execute(
				WorkflowExecutionContext workflowExecutionContext,
				Map<String, Object> parameters) {

				return new com.liferay.support.tools.workflow.spi.WorkflowStepResult(
					true, 1, 1, 0, List.of(Map.of("ok", true)), null,
					Map.of("slug", "demo-site"));
			}

			@Override
			public String operationName() {
				return "custom.data.operation";
			}
		};

		WorkflowFunction workflowFunction = new WorkflowFunctionFactory().create(
			adapter);

		WorkflowStepResult result = workflowFunction.executor().execute(
			new WorkflowStepExecutionRequest(
				"w-1", "s-1", "custom.data.operation", "idem-1", Map.of(),
				new DefaultWorkflowExecutionContext(Map.of(), 1001L, 2002L)));

		assertEquals("demo-site", result.data().get("slug"));
	}

}
