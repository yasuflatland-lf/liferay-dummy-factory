package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowEngineTest {

	@Test
	void executeResolvesInputAndStepReferencesInOrder() throws Exception {
		Map<String, WorkflowFunction> functions = new LinkedHashMap<>();

		functions.put(
			"site.create",
			new DefaultWorkflowFunction(
				new WorkflowFunctionDescriptor(
					"site.create", "create site",
					List.of(
						new WorkflowFunctionParameter(
							"baseName", "string", true, "base name", null)),
					"WorkflowStepResult"),
				request -> new WorkflowStepResult(
					true, 1, 1, 0, null,
					List.of(Map.of("groupId", 101L, "siteName", "Demo Site")),
					Map.of("slug", "demo-site"))));
		functions.put(
			"layout.create",
			new DefaultWorkflowFunction(
				new WorkflowFunctionDescriptor(
					"layout.create", "create layout",
					List.of(
						new WorkflowFunctionParameter(
							"groupId", "long", true, "group id", null),
						new WorkflowFunctionParameter(
							"siteName", "string", true, "site name", null),
						new WorkflowFunctionParameter(
							"slug", "string", true, "slug", null),
						new WorkflowFunctionParameter(
							"pageTitle", "string", true, "page title", null)),
					"WorkflowStepResult"),
				request -> {
					assertEquals(101L, request.parameters().get("groupId"));
					assertEquals("Demo Site", request.parameters().get("siteName"));
					assertEquals("demo-site", request.parameters().get("slug"));
					assertEquals("Welcome", request.parameters().get("pageTitle"));

					return new WorkflowStepResult(
						true, 1, 1, 0, null,
						List.of(Map.of("layoutId", 501L)), Map.of());
				}));

		WorkflowEngine workflowEngine = new WorkflowEngine(
			new MapWorkflowFunctionRegistry(functions));

		WorkflowPlan workflowPlan = new WorkflowPlan(
			new WorkflowDefinition(
				"1.0", "workflow-1", Map.of("pageTitle", "Welcome"),
				List.of(
					new WorkflowStepDefinition(
						"createSite", "site.create", "idem-1",
						List.of(
							new WorkflowParameter(
								"baseName", new WorkflowLiteralValue("Demo"))),
						WorkflowErrorPolicy.FAIL_FAST),
					new WorkflowStepDefinition(
						"createLayout", "layout.create", "idem-2",
						List.of(
							new WorkflowParameter(
								"groupId",
								new WorkflowReferenceValue(
									new WorkflowReferenceParser().parse(
										"steps.createSite.items[0].groupId"))),
							new WorkflowParameter(
								"siteName",
								new WorkflowReferenceValue(
									new WorkflowReferenceParser().parse(
										"steps.createSite.items[0].siteName"))),
							new WorkflowParameter(
								"slug",
								new WorkflowReferenceValue(
									new WorkflowReferenceParser().parse(
										"steps.createSite.data.slug"))),
							new WorkflowParameter(
								"pageTitle",
								new WorkflowReferenceValue(
									new WorkflowReferenceParser().parse(
										"input.pageTitle")))),
						WorkflowErrorPolicy.FAIL_FAST))));

		WorkflowExecutionResult workflowExecutionResult = workflowEngine.execute(
			workflowPlan);

		assertEquals(
			WorkflowExecutionStatus.SUCCEEDED, workflowExecutionResult.status());
		assertEquals(2, workflowExecutionResult.steps().size());
		assertEquals(
			WorkflowStepStatus.SUCCEEDED,
			workflowExecutionResult.steps().get(0).status());
		assertEquals(
			WorkflowStepStatus.SUCCEEDED,
			workflowExecutionResult.steps().get(1).status());
		assertEquals(
			501L,
			workflowExecutionResult.steps().get(1).result().items().get(0).get(
				"layoutId"));
	}

	@Test
	void executeStopsAfterFirstFailedStep() throws Exception {
		Map<String, WorkflowFunction> functions = Map.of(
			"company.create",
			new DefaultWorkflowFunction(
				new WorkflowFunctionDescriptor(
					"company.create", "create company",
					List.of(
						new WorkflowFunctionParameter(
							"count", "integer", true, "count", null)),
					"WorkflowStepResult"),
				request -> new WorkflowStepResult(
					false, 1, 0, 1, "company failed", List.of(), Map.of())),
			"site.create",
			new DefaultWorkflowFunction(
				new WorkflowFunctionDescriptor(
					"site.create", "create site", List.of(), "WorkflowStepResult"),
				request -> new WorkflowStepResult(
					true, 1, 1, 0, null,
					List.of(Map.of("groupId", 200L)), Map.of())));

		WorkflowEngine workflowEngine = new WorkflowEngine(
			new MapWorkflowFunctionRegistry(functions));

		WorkflowPlan workflowPlan = new WorkflowPlan(
			new WorkflowDefinition(
				"1.0", "workflow-2", Map.of(),
				List.of(
					new WorkflowStepDefinition(
						"createCompany", "company.create", "idem-1",
						List.of(
							new WorkflowParameter(
								"count", new WorkflowLiteralValue(1))),
						WorkflowErrorPolicy.FAIL_FAST),
					new WorkflowStepDefinition(
						"createSite", "site.create", "idem-2", List.of(),
						WorkflowErrorPolicy.FAIL_FAST))));

		WorkflowExecutionResult workflowExecutionResult = workflowEngine.execute(
			workflowPlan);

		assertEquals(
			WorkflowExecutionStatus.FAILED, workflowExecutionResult.status());
		assertEquals("createCompany", workflowExecutionResult.failedStepId());
		assertEquals(
			WorkflowStepStatus.FAILED,
			workflowExecutionResult.steps().get(0).status());
		assertEquals(
			WorkflowStepStatus.NOT_EXECUTED,
			workflowExecutionResult.steps().get(1).status());
		assertNotNull(workflowExecutionResult.steps().get(0).error());
	}

	@Test
	void validateRejectsUnknownOperationsAndForwardReferences() {
		WorkflowEngine workflowEngine = new WorkflowEngine(
			new MapWorkflowFunctionRegistry(Map.of()));

		WorkflowPlan workflowPlan = new WorkflowPlan(
			new WorkflowDefinition(
				"1.0", "workflow-3", Map.of(),
				List.of(
					new WorkflowStepDefinition(
						"step-1", "unknown.operation", "idem-1",
						List.of(
							new WorkflowParameter(
								"groupId",
								new WorkflowReferenceValue(
									new WorkflowReferenceParser().parse(
										"steps.step-2.items[0].groupId")))),
						WorkflowErrorPolicy.FAIL_FAST),
					new WorkflowStepDefinition(
						"step-2", "another.unknown", "idem-2", List.of(),
						WorkflowErrorPolicy.FAIL_FAST))));

		List<WorkflowValidationError> errors = workflowEngine.validate(
			workflowPlan);

		assertEquals(3, errors.size());
		assertEquals("UNKNOWN_OPERATION", errors.get(0).code());
		assertEquals("STEP_REFERENCE_ORDER", errors.get(1).code());
		assertEquals("UNKNOWN_OPERATION", errors.get(2).code());
		assertThrows(
			WorkflowValidationException.class,
			() -> workflowEngine.execute(workflowPlan));
	}

}
