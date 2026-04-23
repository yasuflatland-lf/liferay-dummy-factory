package com.liferay.support.tools.workflow.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard step result contract shared by workflow adapters.
 */
public record WorkflowStepResult(
	boolean success,
	long requested,
	long count,
	long skipped,
	List<Map<String, Object>> items,
	String error,
	Map<String, Object> data) {

	public WorkflowStepResult(
		boolean success, long requested, long count, long skipped,
		List<Map<String, Object>> items, String error) {

		this(success, requested, count, skipped, items, error, Map.of());
	}

	public WorkflowStepResult {
		if (requested < 0) {
			throw new IllegalArgumentException("requested must be >= 0");
		}

		if (count < 0) {
			throw new IllegalArgumentException("count must be >= 0");
		}

		if (skipped < 0) {
			throw new IllegalArgumentException("skipped must be >= 0");
		}

		error = ((error == null) || error.isBlank()) ? null : error;
		items = _copyItems(items);
		data = _copyData(data);
	}

	private static Map<String, Object> _copyData(Map<String, Object> data) {
		if ((data == null) || data.isEmpty()) {
			return Map.of();
		}

		return Collections.unmodifiableMap(new LinkedHashMap<>(data));
	}

	private static List<Map<String, Object>> _copyItems(
		List<Map<String, Object>> items) {

		if ((items == null) || items.isEmpty()) {
			return List.of();
		}

		List<Map<String, Object>> copiedItems = new ArrayList<>(items.size());

		for (Map<String, Object> item : items) {
			if (item == null) {
				copiedItems.add(Collections.emptyMap());

				continue;
			}

			copiedItems.add(
				Collections.unmodifiableMap(new LinkedHashMap<>(item)));
		}

		return Collections.unmodifiableList(copiedItems);
	}

}
