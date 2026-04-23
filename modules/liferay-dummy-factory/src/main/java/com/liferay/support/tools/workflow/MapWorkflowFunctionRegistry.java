package com.liferay.support.tools.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class MapWorkflowFunctionRegistry implements WorkflowFunctionRegistry {

	public MapWorkflowFunctionRegistry(Map<String, WorkflowFunction> functions) {
		_functions = Map.copyOf(new LinkedHashMap<>(functions));
	}

	@Override
	public Optional<WorkflowFunction> getFunction(String operation) {
		return Optional.ofNullable(_functions.get(operation));
	}

	private final Map<String, WorkflowFunction> _functions;

}
