package com.motorbesitzen.rolewatcher.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper functions for logging.
 */
public final class LogUtil {

	private final static Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);

	/**
	 * Used to log an information. Uses Spring (slf4j) info logger ({@code Logger.info(...)}).
	 *
	 * @param message The message to log.
	 */
	public static void logInfo(String message) {
		LOGGER.info(message);
	}

	/**
	 * Used to log a warning due to some weird behaviour. Uses Spring (slf4j) info logger ({@code Logger.warn(...)}).
	 *
	 * @param message The message to log.
	 */
	public static void logWarning(String message) {
		LOGGER.warn(message);
	}

	/**
	 * Used to log debug information. Uses Spring (slf4j) debug logger ({@code Logger.debug(...)}).
	 *
	 * @param message The message to print as debug information.
	 */
	public static void logDebug(String message) {
		LOGGER.debug(message);
	}

	/**
	 * Used to log errors. Uses Spring (slf4j) debug logger ({@code Logger.debug(...)}).
	 *
	 * @param message The message to describe the error.
	 */
	public static void logError(String message) {
		LOGGER.error(message);
	}

	/**
	 * Used to log errors and additional info. Uses Spring (slf4j) debug logger ({@code Logger.debug(...)}).
	 *
	 * @param message The message to describe the error.
	 * @param t       The thrown error.
	 */
	public static void logError(String message, Throwable t) {
		LOGGER.error(message, t);
	}

	/**
	 * Used to log debug information and aditional exceptions. Uses Spring (slf4j) debug logger ({@code Logger.debug(...)}).
	 *
	 * @param message The message to print as debug information.
	 */
	public static void logDebug(String message, Throwable t) {
		LOGGER.debug(message, t);
	}
}
