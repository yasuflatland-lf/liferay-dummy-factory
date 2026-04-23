package com.liferay.support.tools.service;

import java.util.List;

public record BatchResult<T>(
	boolean success, int count, int requested, int skipped, List<T> items,
	String error) {

	public BatchResult {
		if (requested <= 0) {
			throw new IllegalArgumentException("requested must be > 0");
		}

		if (count < 0) {
			throw new IllegalArgumentException("count must be >= 0");
		}

		if (skipped < 0) {
			throw new IllegalArgumentException("skipped must be >= 0");
		}

		if (!success && ((error == null) || error.isBlank())) {
			throw new IllegalArgumentException(
				"error must be set when success is false");
		}

		if (success && (count != requested)) {
			throw new IllegalArgumentException(
				"success=true requires count == requested; count=" + count +
					" requested=" + requested);
		}

		items = (items == null) ? List.of() : List.copyOf(items);
	}

	public static <T> BatchResult<T> success(
		int requested, List<T> items, int skipped) {

		int count = (items == null) ? 0 : items.size();

		return new BatchResult<>(true, count, requested, skipped, items, null);
	}

	public static <T> BatchResult<T> failure(
		int requested, List<T> items, int skipped, String error) {

		int count = (items == null) ? 0 : items.size();

		return new BatchResult<>(
			false, count, requested, skipped, items, error);
	}

}
