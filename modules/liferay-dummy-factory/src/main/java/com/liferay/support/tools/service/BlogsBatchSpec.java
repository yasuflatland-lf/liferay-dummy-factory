package com.liferay.support.tools.service;

/**
 * Typed value object that collapses the multi-argument
 * {@code BlogsCreator.create} signature into a single per-batch
 * specification.  Nullable/empty inputs are normalised to safe defaults
 * in the compact constructor so callers never need defensive blocks.
 */
public record BlogsBatchSpec(
	BatchSpec batch,
	long groupId,
	String content,
	String subtitle,
	String description,
	boolean allowPingbacks,
	boolean allowTrackbacks,
	String[] trackbackURLs) {

	public BlogsBatchSpec {
		content = (content == null) ? "" : content;
		subtitle = (subtitle == null) ? "" : subtitle;
		description = (description == null) ? "" : description;
		trackbackURLs = _nullToEmpty(trackbackURLs);
	}

	private static String[] _nullToEmpty(String[] array) {
		return (array == null) ? new String[0] : array;
	}

}
