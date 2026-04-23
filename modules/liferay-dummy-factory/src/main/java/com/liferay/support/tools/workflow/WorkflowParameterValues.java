package com.liferay.support.tools.workflow;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkflowParameterValues {

	public WorkflowParameterValues(Map<String, Object> parameters) {
		_parameters = new LinkedHashMap<>(
			(parameters == null) ? Map.of() : parameters);
	}

	public int optionalInt(String name, int defaultValue) {
		Object value = _parameters.get(name);

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
		Object value = _parameters.get(name);

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

			if (string.equalsIgnoreCase("true")) {
				return true;
			}

			if (string.equalsIgnoreCase("false")) {
				return false;
			}
		}

		throw _invalid(name, "a boolean");
	}

	public <T> T optionalEnum(
		String name, EnumParser<T> enumParser, T defaultValue) {

		String value = optionalString(name, null);

		if ((value == null) || value.isBlank()) {
			return defaultValue;
		}

		return enumParser.parse(value);
	}

	public long optionalLong(String name, long defaultValue) {
		Object value = _parameters.get(name);

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Number number) {
			return number.longValue();
		}

		if (value instanceof String string) {
			if (string.isBlank()) {
				return defaultValue;
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

	public long[] optionalPositiveLongArray(String name) {
		Object value = _parameters.get(name);

		if (value == null) {
			return new long[0];
		}

		List<Object> rawValues = _flattenValues(value);
		List<Long> values = new ArrayList<>();

		for (Object rawValue : rawValues) {
			if (rawValue == null) {
				continue;
			}

			if ((rawValue instanceof String blankString) &&
				blankString.isBlank()) {

				continue;
			}

			if ((rawValue instanceof String string) && string.contains(",")) {
				for (String token : string.split(",")) {
					if (!token.isBlank()) {
						values.add(_positiveLong(name, token.trim()));
					}
				}

				continue;
			}

			values.add(_positiveLong(name, rawValue));
		}

		long[] result = new long[values.size()];

		for (int i = 0; i < values.size(); i++) {
			result[i] = values.get(i);
		}

		return result;
	}

	public String optionalString(String name, String defaultValue) {
		Object value = _parameters.get(name);

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String string) {
			return string;
		}

		throw _invalid(name, "a string");
	}

	public int requireCount() {
		int count = optionalInt("count", 0);

		if (count <= 0) {
			throw new IllegalArgumentException("count must be greater than 0");
		}

		return count;
	}

	public long requirePositiveLong(String name) {
		return _positiveLong(name, _parameters.get(name));
	}

	public String requireText(String name) {
		String value = optionalString(name, null);

		if ((value == null) || value.isBlank()) {
			throw new IllegalArgumentException(name + " is required");
		}

		return value;
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

		return List.of(value);
	}

	private IllegalArgumentException _invalid(String name, String expectedType) {
		return new IllegalArgumentException(name + " must be " + expectedType);
	}

	private long _positiveLong(String name, Object value) {
		long longValue;

		if (value == null) {
			throw new IllegalArgumentException(name + " must be positive");
		}

		if (value instanceof Number number) {
			longValue = number.longValue();
		}
		else if (value instanceof String string) {
			if (string.isBlank()) {
				longValue = Long.MIN_VALUE;
			}
			else {
				try {
					longValue = Long.parseLong(string.trim());
				}
				catch (NumberFormatException numberFormatException) {
					throw _invalid(name, "a positive long");
				}
			}
		}
		else {
			throw _invalid(name, "a positive long");
		}

		if (longValue <= 0) {
			throw new IllegalArgumentException(name + " must be positive");
		}

		return longValue;
	}

	@FunctionalInterface
	public interface EnumParser<T> {

		public T parse(String value);

	}

	private final Map<String, Object> _parameters;

}
