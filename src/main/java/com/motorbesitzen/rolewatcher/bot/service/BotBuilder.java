package com.motorbesitzen.rolewatcher.bot.service;

import com.motorbesitzen.rolewatcher.util.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * Initializes and starts the JDA bot.
 */
@Service
public class BotBuilder implements ApplicationListener<ApplicationReadyEvent> {

	private final RoleUpdater updater;

	@Autowired
	private BotBuilder(final RoleUpdater updater) {
		this.updater = updater;
	}

	/**
	 * Gets called by Spring as late as conceivably possible to indicate that the application is ready.
	 * Starts the RoleUpdater and by that the underlying bot.
	 *
	 * @param event Provided by Spring when the Spring application is ready.
	 */
	@Override
	public void onApplicationEvent(final @NotNull ApplicationReadyEvent event) {
		LogUtil.logInfo("Application ready, starting role updater...");
		updater.start();
	}

}
