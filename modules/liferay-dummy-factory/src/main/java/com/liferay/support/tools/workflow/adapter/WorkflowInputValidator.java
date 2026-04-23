package com.liferay.support.tools.workflow.adapter;

public final class WorkflowInputValidator {

	public static String normalizeText(String value, String defaultValue) {
		if ((value == null) || value.isBlank()) {
			return defaultValue;
		}

		return value;
	}

	public static int requireCount(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException(
				"count must be greater than 0");
		}

		return count;
	}

	public static long requireNonNegativeId(long id, String fieldName) {
		if (id < 0) {
			throw new IllegalArgumentException(
				fieldName + " must be greater than or equal to 0");
		}

		return id;
	}

	public static long requirePositiveId(long id, String fieldName) {
		if (id <= 0) {
			throw new IllegalArgumentException(
				fieldName + " must be greater than 0");
		}

		return id;
	}

	private WorkflowInputValidator() {
	}

}
