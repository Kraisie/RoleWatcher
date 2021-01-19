package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Optional;

/**
 * Removes a Discord user from the whitelist.
 */
public class RemoveWhitelist extends CommandImpl {

	private final DiscordUserRepo dcUserRepo;

	public RemoveWhitelist(final DiscordUserRepo dcUserRepo) {
		this.dcUserRepo = dcUserRepo;
	}

	/**
	 * Removes a Discord user from the whitelist if he already exists in the database and is whitelisted. If he does not
	 * exist this it sends an error message. Also sends an error message if no ID
	 * is given.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final long id = DiscordMessageUtil.getMentionedMemberId(event.getMessage());
		if (id == -1) {
			sendErrorMessage(channel, "Please mention a user or provide a user ID to remove from the whitelist.");
			return;
		}

		Optional<DiscordUser> dcUserOpt = dcUserRepo.findById(id);
		dcUserOpt.ifPresentOrElse(dcUser -> updateWhitelistUser(event, dcUser), () -> sendErrorMessage(channel, "User does no exist!"));
	}

	/**
	 * Sets the whitelist status to false if it is not already false and saves the update to the database.
	 *
	 * @param event  The channel to answer in that the user got removed from the whitelisted.
	 * @param dcUser The {@link DiscordUser} for the given ID.
	 */
	private void updateWhitelistUser(final GuildMessageReceivedEvent event, final DiscordUser dcUser) {
		if (!dcUser.isWhitelisted()) {
			sendErrorMessage(event.getChannel(), "User is not whitelisted!");
			return;
		}

		dcUser.setWhitelisted(false);
		dcUserRepo.save(dcUser);
		answer(event.getChannel(), "Updated whitelist status of user <@" + dcUser.getDiscordId() + ">.");
		doRemoveWhitelistLog(event, dcUser);
	}

	/**
	 * Logs the action that a user got removed from the whitelist.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The whitelisted user.
	 */
	private void doRemoveWhitelistLog(final GuildMessageReceivedEvent event, final DiscordUser user) {
		final String authorId = event.getAuthor().getId();
		final String author = event.getAuthor().getAsTag();
		final String message = author + " (" + authorId + ")" + " removed a user from the whitelist: " + user.getDiscordId() + ".";
		LogUtil.logInfo(message);
	}
}

