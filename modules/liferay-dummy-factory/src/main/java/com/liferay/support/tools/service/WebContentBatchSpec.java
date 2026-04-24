package com.liferay.support.tools.service;

/**
 * Typed value object that collapses the multi-argument
 * {@code WebContentCreator.createSimple / createDummy /
 * createWithStructureTemplate} signatures into a single per-batch
 * specification.  Nullable/empty inputs are normalised to safe defaults
 * in the compact constructor so callers never need defensive blocks.
 *
 * <p>{@code createContentsType} selects the creation strategy:
 * <ul>
 *   <li>0 — simple (uses {@code baseArticle})</li>
 *   <li>1 — dummy / randomised (uses {@code titleWords},
 *       {@code totalParagraphs}, {@code randomAmount},
 *       {@code linkLists})</li>
 *   <li>2 — structure + template (uses {@code ddmStructureId},
 *       {@code ddmTemplateId})</li>
 * </ul>
 */
public record WebContentBatchSpec(
	BatchSpec batch,
	long[] groupIds,
	long folderId,
	String[] locales,
	boolean neverExpire,
	boolean neverReview,
	int createContentsType,
	String baseArticle,
	int titleWords,
	int totalParagraphs,
	int randomAmount,
	String linkLists,
	long ddmStructureId,
	long ddmTemplateId,
	AssetTagNames tags) {

	public WebContentBatchSpec {
		groupIds = _nullToEmpty(groupIds);
		locales = _nullToEmptyStringArray(locales);
		baseArticle = (baseArticle == null) ? "" : baseArticle;
		linkLists = (linkLists == null) ? "" : linkLists;
		tags = (tags == null) ? AssetTagNames.EMPTY : tags;
	}

	private static long[] _nullToEmpty(long[] array) {
		return (array == null) ? new long[0] : array;
	}

	private static String[] _nullToEmptyStringArray(String[] array) {
		return (array == null) ? new String[0] : array;
	}

}
