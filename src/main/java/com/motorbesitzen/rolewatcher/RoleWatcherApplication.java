package com.motorbesitzen.rolewatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main class of the RoleWatcher bot. Starts everything Spring related.
 */
@SpringBootApplication
public class RoleWatcherApplication {

	/**
	 * Starts the application.
	 *
	 * @param args The start arguments.
	 */
	public static void main(final String[] args) {
		SpringApplication.run(RoleWatcherApplication.class, args);
	}
}
