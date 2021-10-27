package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordBanRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Syncs the guild bans to the database.
 */
@Service("syncbans")
class SyncBans extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final DiscordUserRepo userRepo;
	private final DiscordBanRepo banRepo;

	// public to be able to use @Transactional on execute()
	@Autowired
	public SyncBans(final DiscordGuildRepo guildRepo, final DiscordUserRepo userRepo, final DiscordBanRepo banRepo) {
		this.guildRepo = guildRepo;
		this.userRepo = userRepo;
		this.banRepo = banRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "syncbans";
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
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Synchronises bans between Discord and database.";
	}

	/**
	 * Synchronises Discord guild bans with the database bans as restarts or connection losses might get them out of
	 * sync over time.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Transactional
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guild.getIdLong());
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createDiscordGuild(guild.getIdLong()));
		if (!dcGuild.hasBanSyncPerm()) {
			sendErrorMessage(event.getChannel(), "Your guild does not have the required permission!");
			return;
		}

		guild.retrieveBanList().queue(
				banList -> syncBanList(event, banList),
				throwable -> {
					LogUtil.logError("Could not request ban list of " + guild.getName(), throwable);
					sendErrorMessage(event.getChannel(), "Could not request ban list of this guild!");
				}
		);
	}

	/**
	 * Synchronises the ban list of the guild with the one saved in the database. Adds all bans that are not yet
	 * saved in the database.
	 *
	 * @param event   The event provided by JDA that a guild message got received.
	 * @param banList The list of bans of that guild provided by Discord.
	 */
	private void syncBanList(final GuildMessageReceivedEvent event, final List<Guild.Ban> banList) {
		final List<DiscordBan> dcBans = (List<DiscordBan>) banRepo.findAll();
		final List<Guild.Ban> toAdd = getBansToAdd(dcBans, banList);
		event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queue(
				auditLogEntries -> {
					addBans(auditLogEntries, toAdd);
					sendSummary(event.getChannel(), toAdd.size());
				},
				throwable -> {
					LogUtil.logError("Could not request audit log:", throwable);
					addBans(new ArrayList<>(), toAdd);
					sendSummary(event.getChannel(), toAdd.size());
				}
		);
	}

	/**
	 * Collects all bans in the Discord guild that are not yet saved in the database and need to get added.
	 *
	 * @param dcBans  The list of bans saved in the database for the guild.
	 * @param banList The list of bans of that guild provided by Discord.
	 * @return The list of bans to add to the database.
	 */
	private List<Guild.Ban> getBansToAdd(final List<DiscordBan> dcBans, final List<Guild.Ban> banList) {
		final List<Guild.Ban> toAdd = new ArrayList<>();
		for (Guild.Ban ban : banList) {
			final long bannedUserId = ban.getUser().getIdLong();
			if (isSavedBan(dcBans, bannedUserId)) {
				continue;
			}

			toAdd.add(ban);
		}

		return toAdd;
	}

	/**
	 * Checks if a Discord ban is already saved in the database.
	 *
	 * @param dcBans       The list of bans in the database.
	 * @param bannedUserId The Discord ID of the user which has a ban on the guild.
	 * @return {@code true} if the banned user has a ban database entry.
	 */
	private boolean isSavedBan(final List<DiscordBan> dcBans, final long bannedUserId) {
		for (DiscordBan dcBan : dcBans) {
			if (dcBan.getBannedUser().getDiscordId() == bannedUserId) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds a list of missing bans to the database.
	 *
	 * @param auditLogEntries The audit log information.
	 * @param toAdd           The bans to add to the database.
	 */
	private void addBans(final List<AuditLogEntry> auditLogEntries, final List<Guild.Ban> toAdd) {
		final List<DiscordBan> dcBans = new ArrayList<>();
		for (Guild.Ban ban : toAdd) {
			final long bannedUserId = ban.getUser().getIdLong();
			final Optional<DiscordUser> bannedUserOpt = userRepo.findById(bannedUserId);
			final DiscordUser bannedUser = bannedUserOpt.orElseGet(() -> createDiscordUser(bannedUserId));
			final AuditLogEntry matchingEntry = findMatchingEntry(auditLogEntries, bannedUserId);
			final DiscordBan dcBan = matchingEntry == null ?
					DiscordBan.withUnknownActor(ban.getReason(), bannedUser) :
					(
							matchingEntry.getUser() == null ?
									DiscordBan.withUnknownActor(ban.getReason(), bannedUser) :
									DiscordBan.createDiscordBan(matchingEntry.getUser().getIdLong(), ban.getReason(), bannedUser)
					);
			dcBans.add(dcBan);
			LogUtil.logInfo("[SYNC] Add " + dcBan);
		}

		banRepo.saveAll(dcBans);
	}

	/**
	 * Finds a matching audit log entry for a ban of a user if it exists
	 *
	 * @param auditLogEntries The ban audit log entries of the guild.
	 * @param bannedUserId    The Discord ID of the banned user.
	 * @return The audit log entry for the ban of the given user, {@code null} if it can not find one.
	 */
	private AuditLogEntry findMatchingEntry(final List<AuditLogEntry> auditLogEntries, final long bannedUserId) {
		for (AuditLogEntry entry : auditLogEntries) {
			if (entry.getTargetIdLong() == bannedUserId) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Sends a summary of how many bans got added to the database.
	 *
	 * @param channel The channel to send the summary in.
	 * @param addSize The amount of added bans.
	 */
	private void sendSummary(final TextChannel channel, final int addSize) {
		final String added = addSize > 0 ? "Added " + addSize + " " + (addSize > 1 ? "bans" : "ban") : "";
		final String answer = added.isBlank() ? "Bans are already in sync." : added + ".";
		answer(channel, answer);
	}

	/**
	 * Creates a default guild with the given guild ID.
	 *
	 * @param guildId The guild ID to create a guild with.
	 * @return The database entry for the guild.
	 */
	private DiscordGuild createDiscordGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.createDefault(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}

	/**
	 * Creates a Discord user and saves it in the database.
	 *
	 * @param bannedUserId The ID of the user to save.
	 * @return The database entry for the user.
	 */
	private DiscordUser createDiscordUser(final long bannedUserId) {
		final DiscordUser discordUser = DiscordUser.createDiscordUser(bannedUserId);
		userRepo.save(discordUser);
		return discordUser;
	}
}
