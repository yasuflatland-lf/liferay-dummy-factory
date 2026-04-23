package com.liferay.support.tools.workflow.adapter.core.dto;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.SiteMembershipType;

import java.util.Objects;

public record SiteCreateRequest(
	boolean active, BatchSpec batch, String description,
	boolean inheritContent, boolean manualMembership,
	SiteMembershipType membershipType, long parentGroupId,
	long privateLayoutSetPrototypeId, long publicLayoutSetPrototypeId,
	long siteTemplateId) {

	public SiteCreateRequest {
		Objects.requireNonNull(batch, "batch is required");
		description = Objects.requireNonNullElse(description, "");
		membershipType = Objects.requireNonNullElse(
			membershipType, SiteMembershipType.OPEN);
	}

}
