package com.motorbesitzen.rolewatcher.util;

public final class EnvironmentUtil {

	public static String getEnvironmentVariable(final String name) {
		return System.getenv(name);
	}

	public static String getEnvironmentVariableOrDefault(final String name, final String defaultValue) {
		return System.getenv().getOrDefault(name, defaultValue);
	}
}
