package com.liferay.support.tools.workflow.adapter.core.dto;

import com.liferay.support.tools.service.BatchSpec;

import java.util.Objects;

public record OrganizationCreateRequest(
	BatchSpec batch, long parentOrganizationId, boolean site) {

	public OrganizationCreateRequest {
		Objects.requireNonNull(batch, "batch is required");
	}

}
