package com.motorbesitzen.rolewatcher.config;

import com.motorbesitzen.rolewatcher.bot.command.CommandListener;
import com.motorbesitzen.rolewatcher.bot.event.AuthedDeletionListener;
import com.motorbesitzen.rolewatcher.bot.event.BanListener;
import com.motorbesitzen.rolewatcher.bot.event.GuildJoinListener;
import com.motorbesitzen.rolewatcher.bot.event.GuildMemberJoinListener;
import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

/**
 * Provides beans for any JDA related class.
 */
@Configuration
public class JdaBeanConfig {

	/**
	 * Provides the JDA object by starting the bot. If the bot can not be started the application gets stopped.
	 *
	 * @param cmdListener             The class that handles bot commands for the bot.
	 * @param banListener             The class that handles ban and unban events for the bot.
	 * @param authedDeletionListener  The class that handles role and channel deletions for the bot.
	 * @param guildMemberJoinListener The class that handles member join events for the bot.
	 * @param guildJoinListener       The class that handles joining a guild for the bot.
	 * @param applicationContext      The Spring application context.
	 * @return The 'core object' of the bot, the JDA.
	 */
	@Bean
	JDA startBot(final CommandListener cmdListener, final BanListener banListener, final AuthedDeletionListener authedDeletionListener,
				 final GuildMemberJoinListener guildMemberJoinListener, final GuildJoinListener guildJoinListener, final ApplicationContext applicationContext) {
		final String discordToken = getToken(applicationContext);
		final JDABuilder jdaBuilder = buildBot(discordToken, cmdListener, banListener, authedDeletionListener, guildJoinListener, guildMemberJoinListener);
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
	 * @param applicationContext The Spring application context.
	 * @return The token as a {@code String}.
	 */
	private String getToken(ApplicationContext applicationContext) {
		final String discordToken = EnvironmentUtil.getEnvironmentVariable("DC_TOKEN");
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
	 * @param discordToken            The Discord token of the bot.
	 * @param cmdListener             The part of the application handling commands.
	 * @param banListener             The part of the application handling bans and unbans.
	 * @param authedDeletionListener  The part of the application handling role and channel deletions.
	 * @param guildJoinListener       The part of the application handling joins to guilds.
	 * @param guildMemberJoinListener The part of the application handling member joining the guild.
	 * @return A <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/JDA.html">JDA instance</a> of the bot.
	 */
	private JDABuilder buildBot(String discordToken, CommandListener cmdListener, BanListener banListener, AuthedDeletionListener authedDeletionListener, GuildJoinListener guildJoinListener, GuildMemberJoinListener guildMemberJoinListener) {
		Activity activity = getBotActivity();
		return JDABuilder.createDefault(discordToken)
				.enableIntents(GatewayIntent.GUILD_MEMBERS)
				.setStatus(OnlineStatus.ONLINE)
				.setActivity(activity)
				.addEventListeners(cmdListener, banListener, authedDeletionListener, guildJoinListener, guildMemberJoinListener);
	}

	/**
	 * Generates the activity for the bot to display in the Discord member list according
	 * to information in the environment variables. Can be turned off by not including
	 * {@code BOT_ACTIVITY} or {@code BOT_ACTIVITY_TEXT} in the environment file.
	 *
	 * @return A Discord {@code Activity} object.
	 */
	private Activity getBotActivity() {
		String activityType = EnvironmentUtil.getEnvironmentVariable("BOT_ACTIVITY");
		String activityText = EnvironmentUtil.getEnvironmentVariable("BOT_ACTIVITY_TEXT");
		String activityStreamingUrl = EnvironmentUtil.getEnvironmentVariable("BOT_ACTIVITY_STREAMING_URL");

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
	private Activity buildActivity(String type, String text, String url) {
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
	private JDA botLogin(JDABuilder builder) {
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
	private void shutdown(ApplicationContext applicationContext) {
		SpringApplication.exit(applicationContext, () -> 1);
		System.exit(1);
	}
}
