package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Whitelists a Discord user either by Discord ID or by forum ID.
 */
@Service("addwhitelist")
class AddWhitelist extends CommandImpl {

	private final DiscordUserRepo dcUserRepo;

	@Autowired
	private AddWhitelist(final DiscordUserRepo dcUserRepo) {
		this.dcUserRepo = dcUserRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "addwhitelist";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsAuthorization() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsWritePerms() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsReadPerms() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsOwnerPerms() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUsage() {
		return getName() + " (@member|discordid)";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Whitelists a user so his roles do not get updated and he will not be kicked if autokick is enabled. " +
				"Can be used with a Discord tag or a Discord ID.";
	}

	/**
	 * Whitelists a Discord user if he already exists in the database. If he does not exist this method creates a
	 * new entry for that Discord User with the whitelist status set to {@code true}. Sends an error message if no ID
	 * is given. Adds an ID if given as a number even if it does not belong to a user on Discord!
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final long id = DiscordMessageUtil.getMentionedMemberId(event.getMessage());
		if (id == -1) {
			sendErrorMessage(channel, "Please mention a user or provide a user ID to whitelist.");
			return;
		}

		Optional<DiscordUser> dcUserOpt = dcUserRepo.findById(id);
		dcUserOpt.ifPresentOrElse(dcUser -> updateWhitelistUser(event, dcUser), () -> addToWhitelist(event, id));
	}

	/**
	 * Sets the whitelist status to true if it is not already true and saves the update to the database.
	 *
	 * @param event  The event provided by JDA that a guild message got received.
	 * @param dcUser The {@link DiscordUser} for the given ID.
	 */
	private void updateWhitelistUser(final GuildMessageReceivedEvent event, final DiscordUser dcUser) {
		final TextChannel channel = event.getChannel();
		if (dcUser.isWhitelisted()) {
			sendErrorMessage(channel, "User is already whitelisted!");
			return;
		}

		dcUser.setWhitelisted(true);
		dcUserRepo.save(dcUser);
		answer(channel, "Updated whitelist status of user <@" + dcUser.getDiscordId() + ">.");
		doAddWhitelistLog(event, dcUser);
	}

	/**
	 * Creates a new {@link DiscordUser} entry with the whitelist status set to
	 * {@code true} and saves it to the database. The ID does not get checked for being valid!
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param id    The ID to whitelist.
	 */
	private void addToWhitelist(final GuildMessageReceivedEvent event, final long id) {
		final DiscordUser dcUser = DiscordUser.createWhitelistedDiscordUser(id);
		dcUserRepo.save(dcUser);
		answer(event.getChannel(), "Created whitelisted user <@" + dcUser.getDiscordId() + ">.");
		doAddWhitelistLog(event, dcUser);
	}

	/**
	 * Logs the action that a user got whitelisted.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The whitelisted user.
	 */
	private void doAddWhitelistLog(final GuildMessageReceivedEvent event, final DiscordUser user) {
		final String authorId = event.getAuthor().getId();
		final String author = event.getAuthor().getAsTag();
		final String message = author + " (" + authorId + ")" + " whitelisted a user: " + user.getDiscordId() + ".";
		LogUtil.logInfo(message);
	}
}
