package com.liferay.support.tools.workflow.adapter;

import java.lang.reflect.Proxy;
import java.util.Map;

public final class TestModelProxyUtil {

	public static <T> T proxy(Class<T> type, Map<String, Object> values) {
		return type.cast(
			Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type},
				(proxy, method, args) -> {
					String methodName = method.getName();

					if (methodName.equals("equals")) {
						return proxy == args[0];
					}

					if (methodName.equals("hashCode")) {
						return values.hashCode();
					}

					if (methodName.equals("toString")) {
						return type.getSimpleName() + values;
					}

					if (values.containsKey(methodName)) {
						return values.get(methodName);
					}

					return _defaultValue(method.getReturnType());
				}));
	}

	private TestModelProxyUtil() {
	}

	private static Object _defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}

		if (type == boolean.class) {
			return false;
		}

		if (type == byte.class) {
			return (byte)0;
		}

		if (type == char.class) {
			return (char)0;
		}

		if (type == double.class) {
			return 0D;
		}

		if (type == float.class) {
			return 0F;
		}

		if (type == int.class) {
			return 0;
		}

		if (type == long.class) {
			return 0L;
		}

		if (type == short.class) {
			return (short)0;
		}

		return null;
	}

}
