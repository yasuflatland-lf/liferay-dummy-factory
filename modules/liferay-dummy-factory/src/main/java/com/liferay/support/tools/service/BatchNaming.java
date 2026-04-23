package com.liferay.support.tools.service;

public class BatchNaming {

	public static String resolve(String baseName, int count, int index) {
		return resolve(baseName, count, index, "");
	}

	public static String resolve(
			String baseName, int count, int index, String separator) {

		if (count == 1) {
			return baseName;
		}

		return baseName + separator + (index + 1);
	}

}
