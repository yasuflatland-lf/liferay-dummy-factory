package com.liferay.support.tools.service;

public record BatchSpec(int count, String baseName) {

	public static final int MAX_COUNT = 1000;

	public BatchSpec {
		validateCount(count);

		if ((baseName == null) || baseName.isEmpty()) {
			throw new IllegalArgumentException("baseName is required");
		}
	}

	public static void validateCount(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException(
				"count must be greater than 0");
		}

		if (count > MAX_COUNT) {
			throw new IllegalArgumentException(
				"count must be less than or equal to " + MAX_COUNT);
		}
	}

}
