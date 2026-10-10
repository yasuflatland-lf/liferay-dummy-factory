package com.liferay.support.tools.workflow;

import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

import java.util.List;
import java.util.Map;

public class WorkflowFunctionFactory {

	public WorkflowFunction create(
		com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter adapter) {

		return new DefaultWorkflowFunction(
			_descriptorOrGeneric(
				adapter.operationName(), adapter.getClass().getSimpleName()),
			request -> {
				return _invoke(
					() -> _toStepResult(
						adapter.execute(
							new WorkflowExecutionContext(
								_userId(request),
								_runtimeCompanyId(request.context())),
							request.parameters())));
			});
	}

	private static WorkflowFunctionDescriptor _descriptorOrGeneric(
		String operation, String adapterName) {

		WorkflowFunctionDescriptor descriptor = _DESCRIPTORS.get(operation);

		if (descriptor != null) {
			return descriptor;
		}

		return new WorkflowFunctionDescriptor(
			operation,
			"Dynamically registered workflow operation: " + adapterName,
			List.of(), "WorkflowStepResult");
	}

	private static WorkflowStepResult _invoke(
			ThrowingWorkflowStepResultSupplier throwingWorkflowStepResultSupplier)
		throws Exception {

		try {
			return throwingWorkflowStepResultSupplier.get();
		}
		catch (Exception exception) {
			throw exception;
		}
		catch (Throwable throwable) {
			throw new Exception(throwable);
		}
	}

	private static long _runtimeCompanyId(
		WorkflowExecutionContextView workflowExecutionContextView) {

		if (workflowExecutionContextView instanceof DefaultWorkflowExecutionContext
				defaultWorkflowExecutionContext) {

			return defaultWorkflowExecutionContext.currentCompanyId();
		}

		return 0L;
	}

	private static long _runtimeUserId(
		WorkflowExecutionContextView workflowExecutionContextView) {

		if (workflowExecutionContextView instanceof DefaultWorkflowExecutionContext
				defaultWorkflowExecutionContext) {

			return defaultWorkflowExecutionContext.currentUserId();
		}

		return 0L;
	}

	private static WorkflowStepResult _toStepResult(
		com.liferay.support.tools.workflow.spi.WorkflowStepResult result) {

		return new WorkflowStepResult(
			result.success(), (int)result.requested(), (int)result.count(),
			(int)result.skipped(), result.error(), result.items(),
			result.data());
	}

	private static long _userId(WorkflowStepExecutionRequest request) {
		long userId = _runtimeUserId(request.context());

		if (userId <= 0) {
			throw new IllegalArgumentException("userId is required");
		}

		return userId;
	}

	private interface ThrowingWorkflowStepResultSupplier {

		WorkflowStepResult get() throws Throwable;

	}

	private static final Map<String, WorkflowFunctionDescriptor> _DESCRIPTORS =
		WorkflowFunctionDescriptors.descriptors();
}
