package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MapWorkflowFunctionRegistryTest {

	@Test
	void getFunctionReturnsEmptyForUnknownOperation() {
		MapWorkflowFunctionRegistry registry = new MapWorkflowFunctionRegistry(
			Map.of("known.operation", _workflowFunction));

		Optional<WorkflowFunction> function = registry.getFunction(
			"unknown.operation");

		assertFalse(function.isPresent());
	}

	@Test
	void getRequiredFunctionThrowsForUnknownOperation() {
		MapWorkflowFunctionRegistry registry = new MapWorkflowFunctionRegistry(
			Map.of("known.operation", _workflowFunction));

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> registry.getRequiredFunction("unknown.operation"));

		assertEquals(
			"unknown workflow operation: unknown.operation",
			exception.getMessage());
	}

	@Test
	void getFunctionReturnsRegisteredFunction() {
		MapWorkflowFunctionRegistry registry = new MapWorkflowFunctionRegistry(
			Map.of("known.operation", _workflowFunction));

		assertSame(
			_workflowFunction,
			registry.getFunction("known.operation").get());
	}

	private static final WorkflowFunction _workflowFunction = new WorkflowFunction() {

		@Override
		public WorkflowStepExecutor executor() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String operation() {
			return "known.operation";
		}
	};

}
