package com.liferay.support.tools.workflow.adapter.content;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WorkflowParameterValues {

	WorkflowParameterValues(Map<String, Object> parameters) {
		_parameters = (parameters == null) ? Collections.emptyMap() :
			new LinkedHashMap<>(parameters);
	}

	public Object get(String name) {
		return _parameters.get(name);
	}

	public int optionalInt(String name, int defaultValue) {
		Object value = get(name);

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Number number) {
			return number.intValue();
		}

		if (value instanceof String string) {
			if (string.isBlank()) {
				return defaultValue;
			}

			try {
				return Integer.parseInt(string.trim());
			}
			catch (NumberFormatException numberFormatException) {
				throw _invalid(name, "an integer");
			}
		}

		throw _invalid(name, "an integer");
	}

	public boolean optionalBoolean(String name, boolean defaultValue) {
		Object value = get(name);

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}

		if (value instanceof String string) {
			if (string.isBlank()) {
				return defaultValue;
			}

			String normalized = string.trim();

			if (normalized.equals("true")) {
				return true;
			}

			if (normalized.equals("false")) {
				return false;
			}

			throw _invalid(name, "a boolean");
		}

		throw _invalid(name, "a boolean");
	}

	public long optionalLong(String name, long defaultValue) {
		Object value = get(name);

		if (value == null) {
			return defaultValue;
		}

		return _asLong(name, value);
	}

	public String optionalString(String name, String defaultValue) {
		Object value = get(name);

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String string) {
			return string;
		}

		throw _invalid(name, "a string");
	}

	public String[] optionalStringArray(String name) {
		Object value = get(name);

		if (value == null) {
			return new String[0];
		}

		List<String> values = _asStringList(name, value);

		return values.toArray(new String[0]);
	}

	public BatchInput requireBatchInput() {
		int count = optionalInt("count", -1);

		if (count <= 0) {
			throw new IllegalArgumentException(
				"count must be greater than 0");
		}

		String baseName = optionalString("baseName", null);

		if ((baseName == null) || baseName.isBlank()) {
			throw new IllegalArgumentException("baseName is required");
		}

		return new BatchInput(count, baseName);
	}

	public long requirePositiveLong(String name) {
		long value = optionalLong(name, Long.MIN_VALUE);

		if (value <= 0) {
			throw new IllegalArgumentException(name + " must be positive");
		}

		return value;
	}

	public long[] requirePositiveLongArray(String name) {
		Object value = get(name);

		if (value == null) {
			throw new IllegalArgumentException(name + " is required");
		}

		List<Long> values = _asLongList(name, value);

		if (values.isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}

		long[] result = new long[values.size()];

		for (int i = 0; i < values.size(); i++) {
			long longValue = values.get(i);

			if (longValue <= 0) {
				throw new IllegalArgumentException(
					name + " entries must be positive");
			}

			result[i] = longValue;
		}

		return result;
	}

	public record BatchInput(int count, String baseName) {
	}

	private long _asLong(String name, Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}

		if (value instanceof String string) {
			if (string.isBlank()) {
				throw _invalid(name, "a long");
			}

			try {
				return Long.parseLong(string.trim());
			}
			catch (NumberFormatException numberFormatException) {
				throw _invalid(name, "a long");
			}
		}

		throw _invalid(name, "a long");
	}

	private List<Long> _asLongList(String name, Object value) {
		List<Object> rawValues = _flattenValues(value);
		List<Long> longValues = new ArrayList<>(rawValues.size());

		for (Object rawValue : rawValues) {
			longValue:
			{
				if ((rawValue instanceof String string) &&
					string.contains(",")) {

					for (String token : string.split(",")) {
						if (token.isBlank()) {
							continue;
						}

						longValues.add(_asLong(name, token.trim()));
					}

					break longValue;
				}

				longValues.add(_asLong(name, rawValue));
			}
		}

		return longValues;
	}

	private List<String> _asStringList(String name, Object value) {
		List<Object> rawValues = _flattenValues(value);
		List<String> strings = new ArrayList<>(rawValues.size());

		for (Object rawValue : rawValues) {
			if (rawValue == null) {
				continue;
			}

			if (rawValue instanceof String string) {
				if (string.contains(",")) {
					for (String token : string.split(",")) {
						if (!token.isBlank()) {
							strings.add(token.trim());
						}
					}

					continue;
				}

				if (!string.isBlank()) {
					strings.add(string);
				}

				continue;
			}

			throw _invalid(name, "a string or string array");
		}

		return strings;
	}

	private List<Object> _flattenValues(Object value) {
		if (value instanceof Collection<?> collection) {
			return new ArrayList<>(collection);
		}

		if ((value != null) && value.getClass().isArray()) {
			int length = Array.getLength(value);
			List<Object> values = new ArrayList<>(length);

			for (int i = 0; i < length; i++) {
				values.add(Array.get(value, i));
			}

			return values;
		}

		return Collections.singletonList(value);
	}

	private IllegalArgumentException _invalid(String name, String expectedType) {
		return new IllegalArgumentException(
			name + " must be " + expectedType);
	}

	private final Map<String, Object> _parameters;

}
