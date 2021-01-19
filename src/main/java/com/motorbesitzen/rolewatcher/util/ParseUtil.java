package com.motorbesitzen.rolewatcher.util;

/**
 * Helper functions for safely parsing inputs. Since mostly IDs need to be parsed -1 is an anomaly
 * that can be used instead of an error/exception.
 */
public final class ParseUtil {

	/**
	 * Tries to parse a {@code String} to an {@code int}.
	 *
	 * @param integerString The {@code String} representation of a number.
	 * @return The number as {@code int} or -1 if the {@code String} can not be parsed.
	 */
	public static int safelyParseStringToInt(final String integerString) {
		if (integerString == null) {
			return -1;
		}

		try {
			return Integer.parseInt(integerString.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Tries to parse a {@code String} to a {@code long}.
	 *
	 * @param longString The {@code String} representation of a number.
	 * @return The number as {@code long} or -1 if the {@code String} can not be parsed.
	 */
	public static long safelyParseStringToLong(final String longString) {
		if (longString == null) {
			return -1;
		}

		try {
			return Long.parseLong(longString.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
