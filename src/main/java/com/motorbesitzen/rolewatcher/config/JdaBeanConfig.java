package com.motorbesitzen.rolewatcher.config;

import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;
import java.util.Map;

/**
 * Provides beans for any JDA related class.
 */
@Configuration
public class JdaBeanConfig {

	/**
	 * Provides the JDA object by starting the bot. If the bot can not be started the application gets stopped.
	 *
	 * @param envSettings        The class that handles the environment variables.
	 * @param eventListeners     A list of event listeners.
	 * @param applicationContext The Spring application context.
	 * @return The 'core object' of the bot, the JDA.
	 */
	@Bean
	JDA startBot(final EnvSettings envSettings, final Map<String, ? extends ListenerAdapter> eventListeners,
				 final ApplicationContext applicationContext) {
		final String discordToken = getToken(envSettings, applicationContext);
		final JDABuilder jdaBuilder = buildBot(envSettings, discordToken, eventListeners);
		final JDA jda = botLogin(jdaBuilder);
		if (jda == null) {
			shutdown(applicationContext);
			return null;
		}

		return jda;
	}

	/**
	 * Gets the token from the environment variables. Stops the application if no token is set.
	 *
	 * @param envSettings        The class that handles the environment variables.
	 * @param applicationContext The Spring application context.
	 * @return The token as a {@code String}.
	 */
	private String getToken(final EnvSettings envSettings, final ApplicationContext applicationContext) {
		final String discordToken = envSettings.getToken();
		if (discordToken == null) {
			LogUtil.logError("RoleWatcher Discord token is null! Please check the environment variables and add a token.");
			shutdown(applicationContext);
			return null;
		}

		if (discordToken.isBlank()) {
			LogUtil.logError("RoleWatcher Discord token is empty! Please check the environment variables and add a token.");
			shutdown(applicationContext);
			return null;
		}

		return discordToken;
	}

	/**
	 * Initializes the bot with the needed information.
	 *
	 * @param envSettings    The class that handles the environment variables.
	 * @param discordToken   The Discord token of the bot.
	 * @param eventListeners A list of event listeners.
	 * @return A <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/JDA.html">JDA instance</a> of the bot.
	 */
	private JDABuilder buildBot(final EnvSettings envSettings, final String discordToken,
								final Map<String, ? extends ListenerAdapter> eventListeners) {
		final Activity activity = getBotActivity(envSettings);
		final JDABuilder builder =
				JDABuilder.createLight(
						discordToken,
						EnumSet.of(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_MESSAGES)
				).setStatus(OnlineStatus.ONLINE).setActivity(activity);

		for (Map.Entry<String, ? extends ListenerAdapter> eventListener : eventListeners.entrySet()) {
			builder.addEventListeners(eventListener.getValue());
		}

		return builder;
	}

	/**
	 * Generates the activity for the bot to display in the Discord member list according
	 * to information in the environment variables. Can be turned off by not including
	 * {@code BOT_ACTIVITY} or {@code BOT_ACTIVITY_TEXT} in the environment file.
	 *
	 * @param envSettings The class that handles the environment variables.
	 * @return A Discord {@code Activity} object.
	 */
	private Activity getBotActivity(final EnvSettings envSettings) {
		final String activityType = envSettings.getBotActivityType();
		final String activityText = envSettings.getBotActivityText();
		final String activityStreamingUrl = envSettings.getBotStreamingUrl();

		if (activityType == null || activityText == null) {
			LogUtil.logInfo("Activity or activity text not given, ignoring activity settings.");
			return null;
		}

		if (activityType.isBlank() || activityText.isBlank()) {
			LogUtil.logWarning("Activity or activity text not given, ignoring activity settings.");
			return null;
		}

		if (activityType.equalsIgnoreCase("streaming") && activityStreamingUrl == null) {
			LogUtil.logWarning("Streaming activity does not have a stream URL given, ignoring activity settings.");
			return null;
		}

		return buildActivity(activityType, activityText, activityStreamingUrl);
	}

	/**
	 * Generates the {@code Activity} object for the bot to use.
	 *
	 * @param type The activity type.
	 * @param text The text to display next to the activity.
	 * @param url  The URL to the stream, only needed when the {@code ActivityType} is set to 'streaming'.
	 * @return A Discord {@code Activity} object.
	 */
	private Activity buildActivity(final String type, final String text, final String url) {
		switch (type.toLowerCase()) {
			case "playing":
				return Activity.playing(text);
			case "watching":
				return Activity.watching(text);
			case "listening":
				return Activity.listening(text);
			case "streaming":
				return Activity.streaming(text, url);
			case "competing":
				return Activity.competing(text);
			default:
				return Activity.watching("user roles");
		}
	}

	/**
	 * Logs in the bot to the API.
	 *
	 * @param builder The builder that is supposed to generate the JDA instance.
	 * @return The JDA instance, the 'core' of the API/the bot.
	 */
	private JDA botLogin(final JDABuilder builder) {
		try {
			return builder.build();
		} catch (LoginException e) {
			LogUtil.logError("Token is invalid! Please check your token and add a valid Discord token.");
			LogUtil.logDebug(e.getMessage());
		}

		return null;
	}

	/**
	 * Gracefully stops the Spring application and the JVM afterwards.
	 *
	 * @param applicationContext The Spring application context.
	 */
	private void shutdown(final ApplicationContext applicationContext) {
		SpringApplication.exit(applicationContext, () -> 1);
		System.exit(1);
	}
}
