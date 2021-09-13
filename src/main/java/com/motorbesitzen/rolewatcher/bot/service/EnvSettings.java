package com.motorbesitzen.rolewatcher.bot.service;

import com.motorbesitzen.rolewatcher.util.ParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * The class that handles the environment variables.
 */
@Component
public class EnvSettings {

	private final Environment environment;

	@Autowired
	EnvSettings(final Environment environment) {
		this.environment = environment;
	}

	/**
	 * Defines the Discord token that is used to control the bot.
	 *
	 * @return The bot token if there is one set, {@code null} if there is none set.
	 */
	public String getToken() {
		return environment.getProperty("DC_TOKEN");
	}

	/**
	 * Defines the activity the bot shows when online.
	 *
	 * @return The activity as a String if there is one set, {@code null} if there is none set.
	 */
	public String getBotActivityType() {
		return environment.getProperty("BOT_ACTIVITY");
	}

	/**
	 * Defines the text that is used to further elaborate on a bot activity.
	 *
	 * @return The activity text if there is one set, {@code null} if there is none set.
	 */
	public String getBotActivityText() {
		return environment.getProperty("BOT_ACTIVITY_TEXT");
	}

	/**
	 * Defines the streaming URL to set if the bot uses the streaming activity.
	 *
	 * @return The streaming URL if there is one set, {@code null} if there is none set.
	 */
	public String getBotStreamingUrl() {
		return environment.getProperty("BOT_ACTIVITY_STREAMING_URL");
	}

	/**
	 * Defines the command prefix which has to be used to mark a message as an command. A message like
	 * "?help" will get identified as the help command if the command prefix is "?". If no prefix is set there
	 * is no special prefix needed to use a command. Thus "help" would trigger the help command.
	 *
	 * @return The set command prefix if there is one. If there is none set it returns an empty String.
	 */
	public String getCommandPrefix() {
		return environment.getProperty("CMD_PREFIX", "");
	}

	/**
	 * Defines the base URL of a forum profile.
	 *
	 * @return The set forum profile URL base if there is one, {@code null} if there is none set.
	 */
	public String getForumMemberProfileUrl() {
		return environment.getProperty("FORUM_MEMBER_PROFILE_URL");
	}

	/**
	 * Defines the forum role API URL that gets used to request role data about the linked Discord users.
	 *
	 * @return The set forum role API URL if there is one. If there is none set it returns an empty String.
	 */
	public String getForumRoleApiUrl() {
		return environment.getProperty("FORUM_ROLE_API_URL", "");
	}

	/**
	 * Defines the forum role API cooldown between requests that gets used to periodically update the Discord roles
	 * of linked Discord users with the matching forum roles.
	 *
	 * @return The cooldown between forum role API requests in Milliseconds (ms). If there is none set it returns
	 * the default of 5000ms which is 5 seconds between each request.
	 */
	public String getForumRoleApiDelay() {
		return environment.getProperty("FORUM_ROLE_API_DELAY_MS", "5000");
	}

	/**
	 * Defines the key that has to be used when trying to add a user via the bots' API.
	 *
	 * @return The set API verification key if there is one, {@code null} if there is none set.
	 */
	public String getSelfaddApiKey() {
		return environment.getProperty("FORUM_USER_ADD_API_KEY");
	}

	/**
	 * Defines the URL to redirect to if an error occurs.
	 *
	 * @return The URL to redirect to if set or "https://theannoyingsite.com/" if none is set.
	 */
	public String getErrorRedirectUrl() {
		return environment.getProperty("ERROR_REDIRECT_URL", "https://theannoyingsite.com/");
	}

	/**
	 * Defines the ID of the forum role that indicates a ban.
	 *
	 * @return The ID of the banned role if set or an empty String if none is set.
	 */
	public String getForumBannedRoleId() {
		return environment.getProperty("FORUM_BANNED_ROLE_ID", "");
	}

	/**
	 * Defines the color used for embeds in Discord (color bar on the left). If no color is set in the environment
	 * variables it returns orange ({@code #de690c} / {@code rgb(222,105,12)}).
	 *
	 * @return The color to use for embeds.
	 */
	public Color getEmbedColor() {
		final String envR = environment.getProperty("EMBED_COLOR_R", "222");
		final String envG = environment.getProperty("EMBED_COLOR_G", "105");
		final String envB = environment.getProperty("EMBED_COLOR_B", "12");

		final int r = ParseUtil.safelyParseStringToInt(envR);
		final int g = ParseUtil.safelyParseStringToInt(envG);
		final int b = ParseUtil.safelyParseStringToInt(envB);

		// make sure r,g,b stay in rgb range of 0-255
		return new Color(
				Math.max(0, r % 256),
				Math.max(0, g % 256),
				Math.max(0, b % 256)
		);
	}
}
