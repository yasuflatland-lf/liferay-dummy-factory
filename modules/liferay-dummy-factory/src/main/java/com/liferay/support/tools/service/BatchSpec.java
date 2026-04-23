package com.liferay.support.tools.service;

public record BatchSpec(int count, String baseName) {

	public BatchSpec {
		if (count <= 0) {
			throw new IllegalArgumentException(
				"count must be greater than 0");
		}

		if ((baseName == null) || baseName.isEmpty()) {
			throw new IllegalArgumentException("baseName is required");
		}
	}

}
