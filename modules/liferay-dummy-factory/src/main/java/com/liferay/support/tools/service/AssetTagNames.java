package com.liferay.support.tools.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record AssetTagNames(List<String> names) {

	public static final AssetTagNames EMPTY = new AssetTagNames(List.of());

	public AssetTagNames {
		Objects.requireNonNull(names, "names");
		names = List.copyOf(names);
	}

	public static AssetTagNames of(String raw) {
		if ((raw == null) || raw.isBlank()) {
			return EMPTY;
		}

		LinkedHashSet<String> dedup = new LinkedHashSet<>();

		for (String token : raw.split("[,\\r\\n]+")) {
			String normalized = token.trim().toLowerCase(Locale.ROOT);

			if (!normalized.isEmpty()) {
				dedup.add(normalized);
			}
		}

		return new AssetTagNames(new ArrayList<>(dedup));
	}

	public String[] toArray() {
		return names.toArray(new String[0]);
	}

	public boolean isEmpty() {
		return names.isEmpty();
	}

}
