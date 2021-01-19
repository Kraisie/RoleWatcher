package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Optional;

/**
 * Updates a guilds' permission via command in Discord.
 */
public class UpdateGuildPerms extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	public UpdateGuildPerms(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * Updates permissions of a guild. If the caller does not provide a guild ID the current guild will be used.
	 * Permissions can be turned off and on by using false and true, if none is chosen the permission gets disabled.
	 * If there is no guild by the given ID in the database then a new guild gets saved to the database even if
	 * the bot is not in that guild at the moment.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final long mentionedGuildId = DiscordMessageUtil.getMentionedRawId(message) == -1 ? event.getGuild().getIdLong() : DiscordMessageUtil.getMentionedRawId(message);
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(mentionedGuildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> updateGuildPermission(event, dcGuild),
				() -> saveGuild(event, mentionedGuildId)
		);
	}

	/**
	 * Updates a specific permission of a guild. Sends an error message if the command does not include a permission name.
	 *
	 * @param event   The Discord event triggered when a message is received.
	 * @param dcGuild The guild represented as a database object.
	 */
	private void updateGuildPermission(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Message message = event.getMessage();
		final String command = message.getContentRaw();
		final boolean permissionState = command.contains("true");
		final Optional<String> permissionNameOpt = getPermissionName(command);
		permissionNameOpt.ifPresentOrElse(
				permissionName -> saveGuildPermission(event, dcGuild, permissionName, permissionState),
				() -> sendErrorMessage(
						event.getChannel(),
						"Please provide one of the following permission names in quotation marks: \"read\", \"write\", \"sync\", or \"autokick\"."
				)
		);
	}

	/**
	 * Gets the permission name in the raw message.
	 *
	 * @param rawMessage The raw message that the bot received.
	 * @return The permission name if found, {@code Optional.empty()} if not.
	 */
	private Optional<String> getPermissionName(final String rawMessage) {
		if (!rawMessage.matches(".*\".*\".*")) {
			return Optional.empty();
		}

		return Optional.of(rawMessage.substring(rawMessage.indexOf('"') + 1, rawMessage.lastIndexOf('"')));
	}

	/**
	 * Saves the changed permission to the database.
	 *
	 * @param event           The Discord event triggered when a message is received.
	 * @param dcGuild         The guild represented as a database object.
	 * @param permissionName  The name of a permission.
	 * @param permissionState The state of the permission, {@code true} means on and {@code false} means off.
	 */
	private void saveGuildPermission(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild, final String permissionName, final boolean permissionState) {
		if (!isValidPermission(permissionName)) {
			sendErrorMessage(event.getChannel(), "Unknown permission! Please use \"read\", \"write\", \"sync\", or \"autokick\".");
			return;
		}

		setPermission(dcGuild, permissionName, permissionState);
		guildRepo.save(dcGuild);

		String answerText =
				permissionState ?
						"Enabled the \"" + permissionName + "\" permission for the guild with the ID " + dcGuild.getGuildId() + "." :
						"Disabled the \"" + permissionName + "\" permission for the guild with the ID " + dcGuild.getGuildId() + ".";
		answer(event.getChannel(), answerText);
	}

	/**
	 * Checks if the given permission is a valid one that can be updated by this command.
	 *
	 * @param permissionName The name of a permission.
	 * @return {@code true} if the permission is valid, {@code false} otherwise.
	 */
	private boolean isValidPermission(final String permissionName) {
		switch (permissionName.toLowerCase()) {
			case "read":
			case "write":
			case "sync":
			case "autokick":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Sets a permission to the new state for a guild.
	 *
	 * @param dcGuild         The guild represented as a database object.
	 * @param permissionName  The name of a permission.
	 * @param permissionState The state of the permission, {@code true} means on and {@code false} means off.
	 */
	private void setPermission(final DiscordGuild dcGuild, final String permissionName, final boolean permissionState) {
		switch (permissionName.toLowerCase()) {
			case "read":
				dcGuild.setReadPerm(permissionState);
				break;
			case "write":
				dcGuild.setWritePerm(permissionState);
				break;
			case "sync":
				dcGuild.setRoleSyncPerm(permissionState);
				break;
			case "autokick":
				dcGuild.setAutokick(permissionState);
				break;
			default:
				LogUtil.logDebug("Permission name \"" + permissionName + "\" marked as valid but can not be updated!");
		}
	}

	/**
	 * Creates a new guild with default permissions (all off) and enables the given permission.
	 *
	 * @param event            The Discord event triggered when a message is received.
	 * @param mentionedGuildId The ID of the guild to update permissions of.
	 */
	private void saveGuild(final GuildMessageReceivedEvent event, final long mentionedGuildId) {
		final DiscordGuild newGuild = DiscordGuild.createDefault(mentionedGuildId);
		guildRepo.save(newGuild);
		answer(event.getChannel(), "Added mentioned guild to database.");
		updateGuildPermission(event, newGuild);
	}
}
