package com.liferay.support.tools.workflow;

import java.util.ArrayList;
import java.util.List;

public class WorkflowReferenceParser {

	public WorkflowReference parse(String expression) {
		if ((expression == null) || expression.isBlank()) {
			throw new IllegalArgumentException("reference expression is required");
		}

		if (expression.equals("input") || expression.startsWith("input.")) {

			return new WorkflowReference(
				expression, WorkflowReferenceTarget.INPUT, null,
				_segments(expression.substring("input".length())));
		}

		if (expression.startsWith("steps.")) {
			String remaining = expression.substring("steps.".length());
			int delimiter = _delimiter(remaining);
			String stepId = (delimiter == -1) ? remaining :
				remaining.substring(0, delimiter);

			if (stepId.isBlank()) {
				throw new IllegalArgumentException("stepId is required in reference");
			}

			String segmentExpression = (delimiter == -1) ? "" :
				remaining.substring(delimiter);

			if (segmentExpression.startsWith("[")) {
				throw new IllegalArgumentException(
					"reference cannot start with index after stepId");
			}

			return new WorkflowReference(
				expression, WorkflowReferenceTarget.STEP_RESULT, stepId,
				_segments(segmentExpression));
		}

		throw new IllegalArgumentException(
			"reference must start with input or steps.");
	}

	private static int _delimiter(String value) {
		int dotIndex = value.indexOf('.');
		int bracketIndex = value.indexOf('[');

		if (dotIndex == -1) {
			return bracketIndex;
		}

		if (bracketIndex == -1) {
			return dotIndex;
		}

		return Math.min(dotIndex, bracketIndex);
	}

	private static List<WorkflowReferenceSegment> _segments(String expression) {
		List<WorkflowReferenceSegment> segments = new ArrayList<>();
		int index = 0;

		while (index < expression.length()) {
			char current = expression.charAt(index);

			if (current == '.') {
				int start = index + 1;
				int end = start;

				while ((end < expression.length()) &&
					   (expression.charAt(end) != '.') &&
					   (expression.charAt(end) != '[')) {
					end++;
				}

				if (start == end) {
					throw new IllegalArgumentException(
						"reference property is required");
				}

				segments.add(
					new WorkflowReferencePropertySegment(
						expression.substring(start, end)));
				index = end;

				continue;
			}

			if (current == '[') {
				int closingBracket = expression.indexOf(']', index);

				if (closingBracket == -1) {
					throw new IllegalArgumentException("reference index is not closed");
				}

				String token = expression.substring(index + 1, closingBracket);

				if (!token.matches("\\d+")) {
					throw new IllegalArgumentException(
						"reference index must be a non-negative integer");
				}

				segments.add(
					new WorkflowReferenceIndexSegment(Integer.parseInt(token)));
				index = closingBracket + 1;

				continue;
			}

			throw new IllegalArgumentException(
				"reference segment must start with . or [");
		}

		return List.copyOf(segments);
	}

}
