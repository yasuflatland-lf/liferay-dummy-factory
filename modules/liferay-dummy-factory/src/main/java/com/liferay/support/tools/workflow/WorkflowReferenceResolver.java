package com.liferay.support.tools.workflow;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

public class WorkflowReferenceResolver {

	public Object resolve(
		WorkflowReference workflowReference,
		WorkflowExecutionContextView workflowExecutionContextView) {

		Object value = _rootValue(workflowReference, workflowExecutionContextView);

		for (WorkflowReferenceSegment workflowReferenceSegment :
				workflowReference.segments()) {

			value = _segmentValue(
				workflowReference.expression(), value, workflowReferenceSegment);
		}

		return value;
	}

	public Object resolve(
		WorkflowValueSource workflowValueSource,
		WorkflowExecutionContextView workflowExecutionContextView) {

		if (workflowValueSource instanceof WorkflowLiteralValue workflowLiteralValue) {
			return workflowLiteralValue.value();
		}

		if (workflowValueSource instanceof
				WorkflowReferenceValue workflowReferenceValue) {

			return resolve(
				workflowReferenceValue.reference(), workflowExecutionContextView);
		}

		throw new IllegalArgumentException("Unsupported workflow value source");
	}

	private static Object _rootValue(
		WorkflowReference workflowReference,
		WorkflowExecutionContextView workflowExecutionContextView) {

		if (workflowReference.target() == WorkflowReferenceTarget.INPUT) {
			return workflowExecutionContextView.input();
		}

		WorkflowStepResult workflowStepResult =
			workflowExecutionContextView.getStepResult(
				workflowReference.stepId()
			).orElseThrow(
				() -> new IllegalArgumentException(
					"Unknown referenced step: " + workflowReference.stepId())
			);

		return workflowStepResult.asMap();
	}

	private static Object _segmentValue(
		String expression, Object value,
		WorkflowReferenceSegment workflowReferenceSegment) {

		if (workflowReferenceSegment instanceof
				WorkflowReferencePropertySegment
					workflowReferencePropertySegment) {

			if (!(value instanceof Map<?, ?> map)) {
				throw new IllegalArgumentException(
					"Reference " + expression + " expected an object");
			}

			String property = workflowReferencePropertySegment.property();

			if (!map.containsKey(property)) {
				throw new IllegalArgumentException(
					"Reference " + expression + " could not resolve property " +
						property);
			}

			return map.get(property);
		}

		if (workflowReferenceSegment instanceof WorkflowReferenceIndexSegment
				workflowReferenceIndexSegment) {

			int resolvedIndex = workflowReferenceIndexSegment.index();

			if (value instanceof List<?> list) {
				if ((resolvedIndex < 0) || (resolvedIndex >= list.size())) {
					throw new IllegalArgumentException(
						"Reference " + expression + " index out of bounds");
				}

				return list.get(resolvedIndex);
			}

			if ((value != null) && value.getClass().isArray()) {
				int length = Array.getLength(value);

				if ((resolvedIndex < 0) || (resolvedIndex >= length)) {
					throw new IllegalArgumentException(
						"Reference " + expression + " index out of bounds");
				}

				return Array.get(value, resolvedIndex);
			}

			throw new IllegalArgumentException(
				"Reference " + expression + " expected an array or list");
		}

		throw new IllegalArgumentException("Unsupported reference segment");
	}

}
