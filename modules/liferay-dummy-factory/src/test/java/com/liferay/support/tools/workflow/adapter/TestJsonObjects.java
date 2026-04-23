package com.liferay.support.tools.workflow.adapter;

import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;

import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TestJsonObjects {

	public static JSONArray array() {
		return (JSONArray)Proxy.newProxyInstance(
			JSONArray.class.getClassLoader(), new Class<?>[] {JSONArray.class},
			new JsonArrayInvocationHandler());
	}

	public static JSONObject object() {
		return (JSONObject)Proxy.newProxyInstance(
			JSONObject.class.getClassLoader(), new Class<?>[] {JSONObject.class},
			new JsonObjectInvocationHandler());
	}

	private TestJsonObjects() {
	}

	private abstract static class BaseInvocationHandler
		implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

			String name = method.getName();

			if (name.equals("toString")) {
				return _toString();
			}

			if (name.equals("hashCode")) {
				return _hashCode();
			}

			if (name.equals("equals")) {
				return proxy == args[0];
			}

			if (name.equals("writeExternal") || name.equals("readExternal")) {
				return null;
			}

			if (name.equals("write")) {
				return _write((Writer)args[0]);
			}

			if (name.equals("toJSONString")) {
				return _toJSONString();
			}

			if (name.equals("toString") && (method.getParameterCount() == 1)) {
				return _toString();
			}

			return invokeInternal(proxy, method, args);
		}

		protected abstract Object invokeInternal(
			Object proxy, Method method, Object[] args) throws Throwable;

		protected abstract int _hashCode();

		protected abstract String _toJSONString();

		protected abstract String _toString();

		protected Writer _write(Writer writer) throws JSONException {
			return writer;
		}

	}

	private static class JsonArrayInvocationHandler
		extends BaseInvocationHandler {

		@Override
		protected Object invokeInternal(
			Object proxy, Method method, Object[] args)
			throws Throwable {

			String name = method.getName();

			if (name.equals("get")) {
				return _values.get((Integer)args[0]);
			}

			if (name.equals("getJSONObject")) {
				return _values.get((Integer)args[0]);
			}

			if (name.equals("length")) {
				return _values.size();
			}

			if (name.equals("isNull")) {
				return _values.get((Integer)args[0]) == null;
			}

			if (name.equals("iterator")) {
				return _values.iterator();
			}

			if (name.equals("put")) {
				_values.add(args[0]);

				return proxy;
			}

			if (name.equals("join")) {
				return _values.toString();
			}

			if (name.equals("getBoolean")) {
				return (Boolean)_values.get((Integer)args[0]);
			}

			if (name.equals("getDouble")) {
				return ((Number)_values.get((Integer)args[0])).doubleValue();
			}

			if (name.equals("getInt")) {
				return ((Number)_values.get((Integer)args[0])).intValue();
			}

			if (name.equals("getLong")) {
				return ((Number)_values.get((Integer)args[0])).longValue();
			}

			if (name.equals("getString")) {
				Object value = _values.get((Integer)args[0]);

				return (value == null) ? null : value.toString();
			}

			return _defaultValue(method.getReturnType());
		}

		@Override
		protected int _hashCode() {
			return _values.hashCode();
		}

		@Override
		protected String _toJSONString() {
			return _values.toString();
		}

		@Override
		protected String _toString() {
			return _values.toString();
		}

		private final List<Object> _values = new ArrayList<>();

	}

	private static class JsonObjectInvocationHandler
		extends BaseInvocationHandler {

		@Override
		protected Object invokeInternal(
			Object proxy, Method method, Object[] args)
			throws Throwable {

			String name = method.getName();

			if (name.equals("get")) {
				return _values.get((String)args[0]);
			}

			if (name.equals("getJSONArray")) {
				return (JSONArray)_values.get((String)args[0]);
			}

			if (name.equals("getJSONObject")) {
				return (JSONObject)_values.get((String)args[0]);
			}

			if (name.equals("getBoolean")) {
				return (Boolean)_values.get((String)args[0]);
			}

			if (name.equals("getDouble")) {
				return ((Number)_values.get((String)args[0])).doubleValue();
			}

			if (name.equals("getInt")) {
				return ((Number)_values.get((String)args[0])).intValue();
			}

			if (name.equals("getLong")) {
				return ((Number)_values.get((String)args[0])).longValue();
			}

			if (name.equals("getString")) {
				Object value = _values.get((String)args[0]);

				return (value == null) ? null : value.toString();
			}

			if (name.equals("has")) {
				return _values.containsKey((String)args[0]);
			}

			if (name.equals("isNull")) {
				String key = (String)args[0];

				return !_values.containsKey(key) || (_values.get(key) == null);
			}

			if (name.equals("keys")) {
				return _values.keySet().iterator();
			}

			if (name.equals("keySet")) {
				return _values.keySet();
			}

			if (name.equals("length")) {
				return _values.size();
			}

			if (name.equals("names")) {
				return null;
			}

			if (name.equals("opt")) {
				return _values.get((String)args[0]);
			}

			if (name.equals("put")) {
				String key = (String)args[0];
				Object value = args[1];

				if (value instanceof UnsafeSupplier<?, ?> unsafeSupplier) {
					value = unsafeSupplier.get();
				}

				_values.put(key, value);

				return proxy;
			}

			if (name.equals("putException")) {
				_values.put("exception", args[0]);

				return proxy;
			}

			if (name.equals("remove")) {
				return _values.remove((String)args[0]);
			}

			if (name.equals("toMap")) {
				return new LinkedHashMap<>(_values);
			}

			return _defaultValue(method.getReturnType());
		}

		@Override
		protected int _hashCode() {
			return _values.hashCode();
		}

		@Override
		protected String _toJSONString() {
			return _values.toString();
		}

		@Override
		protected String _toString() {
			return _values.toString();
		}

		private final Map<String, Object> _values = new LinkedHashMap<>();

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
