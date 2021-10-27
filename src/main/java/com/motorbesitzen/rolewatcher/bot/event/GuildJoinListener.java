package com.motorbesitzen.rolewatcher.bot.event;

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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Handles guild joins by the bot and adds a guild entry if needed (new guild).
 */
@Service
public class GuildJoinListener extends ListenerAdapter {

	private final DiscordGuildRepo discordGuildRepo;
	private final DiscordBanRepo discordBanRepo;
	private final DiscordUserRepo discordUserRepo;

	@Autowired
	private GuildJoinListener(final DiscordGuildRepo discordGuildRepo, final DiscordBanRepo discordBanRepo, final DiscordUserRepo discordUserRepo) {
		this.discordGuildRepo = discordGuildRepo;
		this.discordBanRepo = discordBanRepo;
		this.discordUserRepo = discordUserRepo;
	}

	/**
	 * If the bot gets added to an unknown guild it creates a default database entry for it in the database.
	 *
	 * @param event The Discord event that the bot joined a guild.
	 */
	@Override
	public void onGuildJoin(final GuildJoinEvent event) {
		final Guild guild = event.getGuild();
		if (discordGuildRepo.existsById(guild.getIdLong())) {
			return;
		}

		final DiscordGuild dcGuild = DiscordGuild.createDefault(guild.getIdLong());
		discordGuildRepo.save(dcGuild);
		LogUtil.logInfo("Added \"" + guild.getName() + "\" (" + guild.getId() + ") to the database with default settings.");

		importBans(guild, dcGuild);
		LogUtil.logInfo("Importing guild bans...");
	}

	/**
	 * Import the ban list of the guild to the database.
	 *
	 * @param guild   The guild the bot got added to.
	 * @param dcGuild The database representation of the guild.
	 */
	private void importBans(final Guild guild, final DiscordGuild dcGuild) {
		guild.retrieveBanList().queue(bans -> handleBanList(guild, dcGuild, bans));
	}

	/**
	 * Iterate over all bans and search for information in the guilds audit logs. Import information if found.
	 * Can only check the audit log 90 days back due to a Discord API limitation!
	 *
	 * @param guild   The guild the bot got added to.
	 * @param dcGuild The database representation of the guild.
	 * @param bans    The list of bans of the guild.
	 */
	private void handleBanList(final Guild guild, final DiscordGuild dcGuild, final List<Guild.Ban> bans) {
		for (Guild.Ban ban : bans) {
			LogUtil.logDebug("Checking audit logs for ban of \"" + ban.getUser().getAsTag() + "\" (" + ban.getUser().getId() + ")...");
			guild.retrieveAuditLogs()
					.type(ActionType.BAN)
					.forEachRemainingAsync(entry -> findBan(dcGuild, ban, entry))
					.thenRunAsync(verifyBanSave(dcGuild, ban));
		}
	}

	/**
	 * Gets called by an async forEach so if a audit log entry matches the ban details it returns false to
	 * stop the forEach checking older audit log entries.
	 *
	 * @param dcGuild The database representation of the guild.
	 * @param ban     The ban representing a banned user in the guild.
	 * @param entry   An audit log entry.
	 * @return {@code true} if the audit log entry does not fit the ban details so the next entry needs to get checked,
	 * {@code false} otherwise to indicate that a fitting audit log entry has been found.
	 */
	private boolean findBan(final DiscordGuild dcGuild, final Guild.Ban ban, final AuditLogEntry entry) {
		if (entry.getTargetId().equals(ban.getUser().getId())) {
			LogUtil.logDebug("Trying to import ban for \"" + ban.getUser().getAsTag() + "\" (" + ban.getUser().getId() + ")...");
			importBan(dcGuild, ban, entry);
			return false;
		}

		return true;
	}

	/**
	 * Imports the information about a ban to the database. Does not save the information if the user already
	 * has a ban entry in the database for the given guild.
	 *
	 * @param dcGuild The database representation of the guild.
	 * @param ban     The ban representing a banned user in the guild.
	 * @param entry   The audit log entry for the ban.
	 */
	private void importBan(final DiscordGuild dcGuild, final Guild.Ban ban, final AuditLogEntry entry) {
		final User actor = entry.getUser();
		final User bannedUser = ban.getUser();
		final long bannedUserId = bannedUser.getIdLong();
		final long actorId = actor == null ? 0 : actor.getIdLong();
		final String reason = entry.getReason() == null ? "No reason given." : entry.getReason();

		if (discordBanRepo.existsByBannedUser_DiscordIdAndGuild_GuildId(bannedUserId, dcGuild.getGuildId())) {
			// if user got banned multiple times and the latest is already in the database skip older
			// bans as the audit log is ordered by new to old.
			LogUtil.logDebug("Database already contains a ban for \"" + bannedUser.getAsTag() + "\" on guild with ID " + dcGuild.getGuildId());
			return;
		}

		final Optional<DiscordUser> dcBannedUserOpt = discordUserRepo.findById(bannedUserId);
		dcBannedUserOpt.ifPresentOrElse(
				dcBannedUser -> discordBanRepo.save(DiscordBan.createDiscordBan(actorId, reason, dcBannedUser)),
				() -> {
					final DiscordUser dcUser = DiscordUser.createDiscordUser(bannedUserId);
					discordUserRepo.save(dcUser);
					discordBanRepo.save(DiscordBan.createDiscordBan(actorId, reason, dcUser));
				}
		);
		LogUtil.logDebug("Imported ban for \"" + ban.getUser().getAsTag() + "\" (" + ban.getUser().getId() + ").");
	}

	/**
	 * If a ban did not get saved before (e.g. when it is older than 90 days and thus the bot can not access the audit
	 * log it will not get saved in {@link #findBan(DiscordGuild, Guild.Ban, AuditLogEntry)}. To make sure all bans get
	 * imported this Runnable saves the ban without additional information from the audit log, thus without the actor of
	 * the ban if the ban did not get added by {@link #findBan(DiscordGuild, Guild.Ban, AuditLogEntry)},
	 *
	 * @param dcGuild The database representation of the guild.
	 * @param ban     The ban representing a banned user in the guild.
	 * @return The Runnable handling the unknown actor bans.
	 */
	private Runnable verifyBanSave(final DiscordGuild dcGuild, final Guild.Ban ban) {
		return () -> {
			final User bannedUser = ban.getUser();
			final long bannedUserId = bannedUser.getIdLong();
			final String reason = ban.getReason() == null ? "No reason given." : ban.getReason();
			if (!discordBanRepo.existsByBannedUser_DiscordIdAndGuild_GuildId(bannedUserId, dcGuild.getGuildId())) {
				final Optional<DiscordUser> dcBannedUserOpt = discordUserRepo.findById(bannedUserId);
				dcBannedUserOpt.ifPresentOrElse(
						dcBannedUser -> discordBanRepo.save(DiscordBan.withUnknownActor(reason, dcBannedUser)),
						() -> {
							DiscordUser dcUser = DiscordUser.createDiscordUser(bannedUserId);
							discordUserRepo.save(dcUser);
							discordBanRepo.save(DiscordBan.withUnknownActor(reason, dcUser));
						}
				);
				LogUtil.logDebug("Imported ban for \"" + ban.getUser().getAsTag() + "\" (" + ban.getUser().getId() + ") with unknown actor.");
			}
		};
	}
}