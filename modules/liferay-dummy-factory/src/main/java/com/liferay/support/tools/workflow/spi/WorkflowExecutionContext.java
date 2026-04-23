package com.liferay.support.tools.workflow.spi;

/**
 * Minimal execution context for workflow step adapters.
 */
public record WorkflowExecutionContext(long userId, long companyId) {

	public WorkflowExecutionContext {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
	}

	public WorkflowExecutionContext(long userId) {
		this(userId, 0L);
	}

}
