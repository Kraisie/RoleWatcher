package com.motorbesitzen.rolewatcher.bot.service;

import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StartUpChecker implements ApplicationListener<ApplicationStartedEvent> {

	private final ApplicationContext context;

	@Autowired
	private StartUpChecker(final ApplicationContext context) {
		this.context = context;
	}

	/**
	 * Performs checks on the most important settings/variables if the application is started. If there are serious
	 * problems the application gets stopped.
	 *
	 * @param event The event provided by Spring that the application started.
	 */
	@Override
	public void onApplicationEvent(final @NotNull ApplicationStartedEvent event) {
		checkCriticalEnvs();
	}

	/**
	 * Checks the most important environment variables. If there are serious problems the application gets stopped.
	 */
	private void checkCriticalEnvs() {
		checkApiKeyEnv();
	}

	/**
	 * Checks that the API key to add users via API is 'secure' by checking length and basic diversity.
	 * Obviously does not check for word lists or dictionary attacks.
	 */
	private void checkApiKeyEnv() {
		final String apiKey = EnvironmentUtil.getEnvironmentVariable("FORUM_USER_ADD_API_KEY");
		if (apiKey == null) {
			LogUtil.logError("Please set a secure (length at least 64) FORUM_USER_ADD_API_KEY in the .env file! Make sure the caller uses an URL encoded key.");
			shutdown();
			return;
		}

		if (apiKey.isBlank() || apiKey.length() < 64) {
			LogUtil.logError("Please set a secure (length at least 64) FORUM_USER_ADD_API_KEY in the .env file! Make sure the caller uses an URL encoded key.");
			shutdown();
		}

		if (apiKey.length() > 1024) {
			// max length of URLs on most systems is 2048 so we make sure the API key doesn't get close to that limit
			LogUtil.logError("It is great that you chose a secure API key but please make sure it is 1024 characters or shorter.");
			shutdown();
		}

		final Map<String, Long> charCount = apiKey.codePoints().mapToObj(Character::toString).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		final Map.Entry<String, Long> mostUsedChar = Collections.max(charCount.entrySet(), Map.Entry.comparingByValue());
		final double mostUsedCharPercentage = (double) mostUsedChar.getValue() / (double) apiKey.length();
		if (mostUsedCharPercentage >= 0.25) {
			LogUtil.logError("Please diversify your API Key contents so you do not use the same characters too often.");
			shutdown();
		}
	}

	/**
	 * Gracefully stops the Spring application and the JVM afterwards.
	 */
	private void shutdown() {
		SpringApplication.exit(context, () -> 1);
		System.exit(1);
	}
}
