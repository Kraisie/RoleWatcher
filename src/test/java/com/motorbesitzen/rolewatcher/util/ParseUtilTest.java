package com.motorbesitzen.rolewatcher.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseUtilTest {

	@Test
	@DisplayName("should parse \"0\" to int 0")
	void testParseZeroToInt() {
		int result = ParseUtil.safelyParseStringToInt("0");

		assertThat(result).isEqualTo(0);
	}

	@Test
	@DisplayName("should parse \"1337\" to int 1337")
	void testParsePositiveNumberToInt() {
		int result = ParseUtil.safelyParseStringToInt("1337");

		assertThat(result).isEqualTo(1337);
	}

	@Test
	@DisplayName("should parse a String with leading and trailing spaces")
	void testParsePositiveNumberWithLeadingAndTrailingSpacesToInt() {
		int result = ParseUtil.safelyParseStringToInt("   1337 ");

		assertThat(result).isEqualTo(1337);
	}

	@Test
	@DisplayName("should parse \"-1337\" to int 1337")
	void testParseNegativeNumberToInt() {
		int result = ParseUtil.safelyParseStringToInt("-1337");

		assertThat(result).isEqualTo(-1337);
	}

	@Test
	@DisplayName("should parse the String of the maximum integer")
	void testParseMaxIntToInt() {
		String maxIntString = String.valueOf(Integer.MAX_VALUE);

		int result = ParseUtil.safelyParseStringToInt(maxIntString);

		assertThat(result).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	@DisplayName("should parse the String of the minimum integer")
	void testParseMinIntToInt() {
		String maxIntString = String.valueOf(Integer.MIN_VALUE);

		int result = ParseUtil.safelyParseStringToInt(maxIntString);

		assertThat(result).isEqualTo(Integer.MIN_VALUE);
	}

	@Test
	@DisplayName("should return -1 on illegal number format (spaces in number)")
	void testSpacesInNumberToInt() {
		int result = ParseUtil.safelyParseStringToInt("12 34");

		assertThat(result).isEqualTo(-1);
	}

	@Test
	@DisplayName("should return -1 on illegal number format (characters in number)")
	void testCharactersInNumberToInt() {
		int result = ParseUtil.safelyParseStringToInt("1a4");

		assertThat(result).isEqualTo(-1);
	}

	@Test
	@DisplayName("should return -1 on empty String")
	void testEmptyStringToInt() {
		int result = ParseUtil.safelyParseStringToInt("");

		assertThat(result).isEqualTo(-1);
	}

	@Test
	@DisplayName("should return -1 on null String")
	void testNullStringToInt() {
		int result = ParseUtil.safelyParseStringToInt(null);

		assertThat(result).isEqualTo(-1);
	}

	@Test
	@DisplayName("should parse \"0\" to long 0")
	void testParseZeroToLong() {
		long result = ParseUtil.safelyParseStringToLong("0");

		assertThat(result).isEqualTo(0L);
	}

	@Test
	@DisplayName("should parse \"1337\" to long 1337")
	void testParsePositiveNumberToLong() {
		long result = ParseUtil.safelyParseStringToLong("1337");

		assertThat(result).isEqualTo(1337L);
	}

	@Test
	@DisplayName("should parse a String with leading and trailing spaces")
	void testParsePositiveNumberWithLeadingAndTrailingSpacesToLong() {
		long result = ParseUtil.safelyParseStringToLong("   1337 ");

		assertThat(result).isEqualTo(1337L);
	}

	@Test
	@DisplayName("should parse \"-1337\" to long 1337")
	void testParseNegativeNumberToLong() {
		long result = ParseUtil.safelyParseStringToLong("-1337");

		assertThat(result).isEqualTo(-1337L);
	}

	@Test
	@DisplayName("should parse the String of the maximum long")
	void testParseMaxIntToLong() {
		String maxLongString = String.valueOf(Long.MAX_VALUE);

		long result = ParseUtil.safelyParseStringToLong(maxLongString);

		assertThat(result).isEqualTo(Long.MAX_VALUE);
	}

	@Test
	@DisplayName("should parse the String of the minimum long")
	void testParseMinIntToLong() {
		String maxLongString = String.valueOf(Long.MIN_VALUE);

		long result = ParseUtil.safelyParseStringToLong(maxLongString);

		assertThat(result).isEqualTo(Long.MIN_VALUE);
	}

	@Test
	@DisplayName("should return -1 on illegal number format (spaces in number)")
	void testSpacesInNumberToLong() {
		long result = ParseUtil.safelyParseStringToLong("12 34");

		assertThat(result).isEqualTo(-1L);
	}

	@Test
	@DisplayName("should return -1 on illegal number format (characters in number)")
	void testCharactersInNumberToLong() {
		long result = ParseUtil.safelyParseStringToLong("1a4");

		assertThat(result).isEqualTo(-1L);
	}

	@Test
	@DisplayName("should return -1 on empty String")
	void testEmptyStringToLong() {
		long result = ParseUtil.safelyParseStringToLong("");

		assertThat(result).isEqualTo(-1L);
	}

	@Test
	@DisplayName("should return -1 on null String")
	void testNullStringToLong() {
		long result = ParseUtil.safelyParseStringToLong(null);

		assertThat(result).isEqualTo(-1L);
	}
}
