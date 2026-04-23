package com.liferay.support.tools.workflow.adapter.core.dto;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.RoleType;

import java.util.Objects;

public record RoleCreateRequest(
	BatchSpec batch, String description, RoleType roleType) {

	public RoleCreateRequest {
		Objects.requireNonNull(batch, "batch is required");
		description = Objects.requireNonNullElse(description, "");
		roleType = Objects.requireNonNullElse(roleType, RoleType.REGULAR);
	}

}
