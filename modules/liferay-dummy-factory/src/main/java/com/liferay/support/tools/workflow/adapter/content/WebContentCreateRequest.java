package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.service.AssetTagNames;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.WebContentBatchSpec;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

import java.util.Locale;

record WebContentCreateRequest(long userId, WebContentBatchSpec batchSpec) {

	static WebContentCreateRequest from(
			WorkflowExecutionContext workflowExecutionContext,
			WorkflowParameterValues workflowParameterValues, Portal portal)
		throws PortalException {

		WorkflowParameterValues.BatchInput batchInput =
			workflowParameterValues.requireBatchInput();
		long[] groupIds = workflowParameterValues.requirePositiveLongArray(
			"groupIds");

		return new WebContentCreateRequest(
			workflowExecutionContext.userId(),
			new WebContentBatchSpec(
				new BatchSpec(batchInput.count(), batchInput.baseName()),
				groupIds,
				workflowParameterValues.optionalLong("folderId", 0L),
				_resolveLocales(workflowParameterValues, portal, groupIds[0]),
				workflowParameterValues.optionalBoolean("neverExpire", true),
				workflowParameterValues.optionalBoolean("neverReview", true),
				workflowParameterValues.optionalInt("createContentsType", 0),
				workflowParameterValues.optionalString("baseArticle", ""),
				workflowParameterValues.optionalInt("titleWords", 5),
				workflowParameterValues.optionalInt("totalParagraphs", 3),
				workflowParameterValues.optionalInt("randomAmount", 3),
				workflowParameterValues.optionalString("linkLists", ""),
				workflowParameterValues.optionalLong("ddmStructureId", 0L),
				workflowParameterValues.optionalLong("ddmTemplateId", 0L),
				AssetTagNames.EMPTY));
	}

	private static String[] _resolveLocales(
			WorkflowParameterValues workflowParameterValues, Portal portal,
			long groupId)
		throws PortalException {

		String[] locales = workflowParameterValues.optionalStringArray(
			"locales");

		if (locales.length > 0) {
			return locales;
		}

		Locale defaultLocale = (portal == null) ? LocaleUtil.getDefault() :
			portal.getSiteDefaultLocale(groupId);

		return new String[] {LocaleUtil.toLanguageId(defaultLocale)};
	}

}
