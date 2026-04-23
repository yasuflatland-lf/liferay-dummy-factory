package com.liferay.support.tools.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WorkflowEngine {

	public WorkflowEngine(WorkflowFunctionRegistry workflowFunctionRegistry) {
		_workflowFunctionRegistry = Objects.requireNonNull(
			workflowFunctionRegistry, "workflowFunctionRegistry is required");
	}

	public WorkflowExecutionResult execute(WorkflowPlan workflowPlan)
		throws WorkflowValidationException {

		return execute(workflowPlan, 0L, 0L);
	}

	public WorkflowExecutionResult execute(
			WorkflowPlan workflowPlan, long currentUserId, long currentCompanyId)
		throws WorkflowValidationException {

		Objects.requireNonNull(workflowPlan, "workflowPlan is required");

		List<WorkflowValidationError> errors = validate(workflowPlan);

		if (!errors.isEmpty()) {
			throw new WorkflowValidationException(errors);
		}

		DefaultWorkflowExecutionContext workflowExecutionContext =
			new DefaultWorkflowExecutionContext(
				workflowPlan.definition().input(), currentUserId,
				currentCompanyId);
		List<WorkflowStepExecutionResult> stepExecutionResults = new ArrayList<>();
		String failedStepId = null;

		for (WorkflowStepDefinition workflowStepDefinition :
				workflowPlan.orderedSteps()) {

			WorkflowFunction workflowFunction =
				_workflowFunctionRegistry.getRequiredFunction(
					workflowStepDefinition.operation());

			try {
				WorkflowStepResult workflowStepResult = workflowFunction.executor(
				).execute(
					new WorkflowStepExecutionRequest(
						workflowPlan.definition().workflowId(),
						workflowStepDefinition.id(),
						workflowStepDefinition.operation(),
						workflowStepDefinition.idempotencyKey(),
						_resolveParameters(
							workflowStepDefinition.parameters(),
							workflowExecutionContext),
						workflowExecutionContext));

				if (workflowStepResult.success()) {
					workflowExecutionContext.putStepResult(
						workflowStepDefinition.id(), workflowStepResult);
					stepExecutionResults.add(
						WorkflowStepExecutionResult.succeeded(
							workflowStepDefinition.id(),
							workflowStepDefinition.operation(),
							workflowStepResult));

					continue;
				}

				WorkflowError workflowError = new WorkflowError(
					"STEP_REPORTED_FAILURE",
					(workflowStepResult.error() == null) ?
						"Workflow step reported failure." :
						workflowStepResult.error());

				stepExecutionResults.add(
					WorkflowStepExecutionResult.failed(
						workflowStepDefinition.id(),
						workflowStepDefinition.operation(), workflowError,
						workflowStepResult));

				failedStepId = workflowStepDefinition.id();

				break;
			}
			catch (Exception exception) {
				WorkflowError workflowError = new WorkflowError(
					"STEP_EXECUTION_FAILED", _message(exception));

				stepExecutionResults.add(
					WorkflowStepExecutionResult.failed(
						workflowStepDefinition.id(),
						workflowStepDefinition.operation(), workflowError, null));

				failedStepId = workflowStepDefinition.id();

				break;
			}
		}

		if (failedStepId != null) {
			_addNotExecutedSteps(
				workflowPlan.orderedSteps(), stepExecutionResults, failedStepId);

			return new WorkflowExecutionResult(
				workflowPlan.definition().workflowId(),
				WorkflowExecutionStatus.FAILED, stepExecutionResults,
				failedStepId);
		}

		return new WorkflowExecutionResult(
			workflowPlan.definition().workflowId(),
			WorkflowExecutionStatus.SUCCEEDED, stepExecutionResults, null);
	}

	public List<WorkflowValidationError> validate(WorkflowPlan workflowPlan) {
		Objects.requireNonNull(workflowPlan, "workflowPlan is required");

		List<WorkflowValidationError> errors = new ArrayList<>();
		Map<String, Integer> stepIndexes = new LinkedHashMap<>();
		List<WorkflowStepDefinition> orderedSteps = workflowPlan.orderedSteps();

		for (int i = 0; i < orderedSteps.size(); i++) {
			WorkflowStepDefinition workflowStepDefinition = orderedSteps.get(i);
			Integer existingIndex = stepIndexes.putIfAbsent(
				workflowStepDefinition.id(), i);

			if (existingIndex != null) {
				errors.add(
					new WorkflowValidationError(
						"DUPLICATE_STEP_ID", "/steps/" + i + "/id",
						"Step id must be unique."));
			}
		}

		for (int i = 0; i < orderedSteps.size(); i++) {
			WorkflowStepDefinition workflowStepDefinition = orderedSteps.get(i);
			Set<String> parameterNames = new LinkedHashSet<>();
			WorkflowFunction workflowFunction =
				_workflowFunctionRegistry.getFunction(
					workflowStepDefinition.operation()
				).orElse(
					null
				);

			if (workflowFunction == null) {
				errors.add(
					new WorkflowValidationError(
						"UNKNOWN_OPERATION", "/steps/" + i + "/operation",
						"Unknown workflow operation: " +
							workflowStepDefinition.operation()));
			}

			for (int j = 0; j < workflowStepDefinition.parameters().size(); j++) {
				WorkflowParameter workflowParameter =
					workflowStepDefinition.parameters().get(j);

				if (!parameterNames.add(workflowParameter.name())) {
					errors.add(
						new WorkflowValidationError(
							"DUPLICATE_PARAMETER_NAME",
							"/steps/" + i + "/params/" + j + "/name",
							"Parameter name must be unique within a step."));
				}

				if (workflowParameter.source() instanceof WorkflowReferenceValue
						workflowReferenceValue) {

					_validateReference(
						workflowReferenceValue.reference(), i, j, stepIndexes,
						errors);
				}
			}

			if (workflowFunction == null) {
				continue;
			}

			try {
				workflowFunction.validateStep(workflowStepDefinition);
			}
			catch (IllegalArgumentException illegalArgumentException) {
				errors.add(
					new WorkflowValidationError(
						"INVALID_STEP", "/steps/" + i,
						illegalArgumentException.getMessage()));
			}
		}

		return List.copyOf(errors);
	}

	private static void _addNotExecutedSteps(
		List<WorkflowStepDefinition> orderedSteps,
		List<WorkflowStepExecutionResult> stepExecutionResults,
		String failedStepId) {

		boolean addRemaining = false;

		for (WorkflowStepDefinition workflowStepDefinition : orderedSteps) {
			if (addRemaining) {
				stepExecutionResults.add(
					WorkflowStepExecutionResult.notExecuted(
						workflowStepDefinition.id(),
						workflowStepDefinition.operation()));
			}

			if (workflowStepDefinition.id().equals(failedStepId)) {
				addRemaining = true;
			}
		}
	}

	private static String _message(Throwable throwable) {
		String message = throwable.getMessage();

		if ((message == null) || message.isBlank()) {
			return throwable.getClass().getSimpleName();
		}

		return message;
	}

	private static Map<String, Object> _resolveParameters(
		List<WorkflowParameter> workflowParameters,
		DefaultWorkflowExecutionContext workflowExecutionContext) {

		Map<String, Object> parameters = new LinkedHashMap<>();

		for (WorkflowParameter workflowParameter : workflowParameters) {
			parameters.put(
				workflowParameter.name(),
				_RESOLVER.resolve(workflowParameter.source(), workflowExecutionContext));
		}

		return Collections.unmodifiableMap(parameters);
	}

	private static void _validateReference(
		WorkflowReference workflowReference, int stepIndex, int parameterIndex,
		Map<String, Integer> stepIndexes, List<WorkflowValidationError> errors) {

		if (workflowReference.target() != WorkflowReferenceTarget.STEP_RESULT) {
			return;
		}

		Integer referencedStepIndex = stepIndexes.get(workflowReference.stepId());

		if (referencedStepIndex == null) {
			errors.add(
				new WorkflowValidationError(
					"UNKNOWN_STEP_REFERENCE",
					"/steps/" + stepIndex + "/params/" + parameterIndex + "/from",
					"Referenced step does not exist: " + workflowReference.stepId()));

			return;
		}

		if (referencedStepIndex >= stepIndex) {
			errors.add(
				new WorkflowValidationError(
					"STEP_REFERENCE_ORDER",
					"/steps/" + stepIndex + "/params/" + parameterIndex + "/from",
					"Step references must target an earlier step."));
		}
	}

	private static final WorkflowReferenceResolver _RESOLVER =
		new WorkflowReferenceResolver();

	private final WorkflowFunctionRegistry _workflowFunctionRegistry;

}
