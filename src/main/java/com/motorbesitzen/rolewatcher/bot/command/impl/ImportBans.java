package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Imports bans of another guild to the list of bans the calling guild has.
 */
@Service("importbans")
class ImportBans extends CommandImpl {

	private final DiscordGuildRepo discordGuildRepo;

	// public to be able to use @Transactional on execute()
	@Autowired
	public ImportBans(final DiscordGuildRepo discordGuildRepo) {
		this.discordGuildRepo = discordGuildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "importbans";
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
		return false;
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
		return getName() + " guildid";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Imports all saved bans from another guild and bans matching users.";
	}

	/**
	 * Add the list of bans of another guild to the callers guild list of bans.
	 * Sends an error message if there is no guild ID given or if the guild ID is the callers guild ID. Also
	 * sends an error message if a guild by the provided ID does not exist in the database.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Transactional
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final Message message = event.getMessage();
		final long mentionedGuildId = DiscordMessageUtil.getMentionedRawId(message);
		if (mentionedGuildId == -1) {
			sendErrorMessage(channel, "Please provide a guild ID to import bans from.");
			return;
		}

		if (mentionedGuildId == event.getGuild().getIdLong()) {
			sendErrorMessage(channel, "Please do not provide your own guild ID.");
			return;
		}

		final Optional<DiscordGuild> discordGuildOpt = discordGuildRepo.findById(mentionedGuildId);
		discordGuildOpt.ifPresentOrElse(
				discordGuild -> importBans(event, discordGuild),
				() -> sendErrorMessage(channel, "Can not find guild with ID " + mentionedGuildId + ". Make sure your ID is correct and try again.")
		);
	}

	/**
	 * Imports the ban list of the given guild to the callers guild ban list and bans all members (users in the callers guild)
	 * that are in that imported list.
	 *
	 * @param event        The Discord event when a message is received.
	 * @param discordGuild The Discord guild to import the bans of.
	 */
	private void importBans(final GuildMessageReceivedEvent event, final DiscordGuild discordGuild) {
		final Guild guild = event.getGuild();
		final Optional<DiscordGuild> callerDcGuildOpt = discordGuildRepo.findById(guild.getIdLong());
		final DiscordGuild callerDcGuild = callerDcGuildOpt.orElseGet(() -> createDiscordGuild(guild.getIdLong()));
		final Set<DiscordBan> importedBans = getImportedBans(discordGuild);
		callerDcGuild.addBans(importedBans);
		discordGuildRepo.save(callerDcGuild);

		final TextChannel channel = event.getChannel();
		answer(channel, "Imported **" + importedBans.size() + "** ban(s) from guild " + discordGuild.getGuildId() + ".");
		LogUtil.logInfo("\"" + guild.getName() + "\" (" + guild.getId() + ") imported " + importedBans.size() + " bans " +
				"from guild with ID " + discordGuild.getGuildId() + ".");

		banMembers(channel, importedBans);
	}

	/**
	 * Retrieves the ban list of the given guild. If that guild already imported a ban the reason gets updated
	 * to identify the guild the ban was last imported from.
	 *
	 * @param discordGuild The Discord guild to import the bans of.
	 * @return A {@code Set<{@link DiscordBan}>} of the old guild.
	 */
	private Set<DiscordBan> getImportedBans(final DiscordGuild discordGuild) {
		final Set<DiscordBan> newBans = new HashSet<>();
		final Set<DiscordBan> importedBans = discordGuild.getBans();
		for (DiscordBan ban : importedBans) {
			String reason = ban.getReason();
			if (reason.startsWith("[IB")) {
				reason = reason.substring(reason.indexOf(':') + 1).trim(); // "[IB123456789012345678]: reason"
			}

			final DiscordBan newBan = DiscordBan.createDiscordBan(
					ban.getActorDiscordId(),
					"[IB" + discordGuild.getGuildId() + "]: " + reason,
					ban.getBannedUser()
			);
			newBans.add(newBan);
		}

		return newBans;
	}

	/**
	 * Bans all members in the caller guild whose bans got imported from the old guild if they are a member of that guild.
	 * Sends a message in the caller channel and updates it if finished.
	 *
	 * @param channel      The caller channel.
	 * @param importedBans The list of imported bans.
	 */
	private void banMembers(final TextChannel channel, final Set<DiscordBan> importedBans) {
		final Message message = answerPlaceholder(channel, "Banning members in imported ban list...");
		final Guild guild = channel.getGuild();
		for (DiscordBan ban : importedBans) {
			guild.retrieveMemberById(ban.getBannedUser().getDiscordId()).queue(
					member -> member.ban(0, ban.getReason()).queue(),
					throwable -> LogUtil.logDebug("Banned member not present in guild.")
			);
		}

		message.editMessage("Banned all members in the imported ban list who were in this guild.").queue();
	}

	/**
	 * Creates a default guild with the given guild ID.
	 *
	 * @param guildId The guild ID to create a guild with.
	 * @return The database entry for the guild.
	 */
	private DiscordGuild createDiscordGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.createDefault(guildId);
		discordGuildRepo.save(dcGuild);
		return dcGuild;
	}
}
