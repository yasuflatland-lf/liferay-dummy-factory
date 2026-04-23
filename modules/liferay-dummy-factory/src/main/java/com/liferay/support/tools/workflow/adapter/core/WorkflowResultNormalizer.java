package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.BatchResult;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class WorkflowResultNormalizer {

	public static <T> WorkflowStepResult normalize(
		BatchResult<T> result,
		Function<T, Map<String, Object>> itemMapper) {

		List<Map<String, Object>> items = new ArrayList<>(result.items().size());

		for (T item : result.items()) {
			items.add(itemMapper.apply(item));
		}

		return new WorkflowStepResult(
			result.success(), result.requested(), result.count(),
			result.skipped(), items, result.error());
	}

	private WorkflowResultNormalizer() {
	}

}
