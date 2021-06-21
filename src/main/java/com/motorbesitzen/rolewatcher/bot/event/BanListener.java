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
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Handles Discord ban and unban events ans saves them to sync bans between multiple forum related guilds.
 */
@Service
public class BanListener extends ListenerAdapter {

	private final DiscordBanRepo discordBanRepo;
	private final DiscordGuildRepo discordGuildRepo;
	private final DiscordUserRepo discordUserRepo;

	@Autowired
	private BanListener(final DiscordBanRepo discordBanRepo, final DiscordGuildRepo discordGuildRepo, final DiscordUserRepo discordUserRepo) {
		this.discordBanRepo = discordBanRepo;
		this.discordGuildRepo = discordGuildRepo;
		this.discordUserRepo = discordUserRepo;
	}

	/**
	 * Handles a guild ban event and saves a Discord ban for it in the database.
	 *
	 * @param event The ban event triggered by Discord.
	 */
	@Override
	public void onGuildBan(final GuildBanEvent event) {
		final User bannedUser = event.getUser();
		final Guild guild = event.getGuild();
		if (discordBanRepo.existsByBannedUser_DiscordIdAndGuild_GuildId(bannedUser.getIdLong(), guild.getIdLong())) {
			// ban already in database e.g. by importing other bans -> ignore
			return;
		}

		guild.retrieveBan(bannedUser).queueAfter(
				15, TimeUnit.SECONDS,
				ban -> checkAuditLogs(event, ban),
				throwable -> LogUtil.logWarning("Could not retrieve ban, maybe already unbanned? " + throwable.getMessage())
		);
	}

	/**
	 * Checks the audit log for more information about the ban.
	 *
	 * @param event The ban event triggered by Discord.
	 * @param ban   The ban object containing the banned user and the reason of the ban.
	 */
	private void checkAuditLogs(final GuildBanEvent event, final Guild.Ban ban) {
		event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queue(auditLogEntries -> findBan(event, ban, auditLogEntries));
	}

	/**
	 * Matches all ban audit logs to the ban information, if there is a match the actor is known and can be saved.
	 *
	 * @param event           The ban event triggered by Discord.
	 * @param ban             The ban object containing the banned user and the reason of the ban.
	 * @param auditLogEntries The list of audit log entries.
	 */
	private void findBan(final GuildBanEvent event, final Guild.Ban ban, final List<AuditLogEntry> auditLogEntries) {
		if (auditLogEntries.size() == 0) {
			handleBan(event.getGuild(), ban);
			return;
		}

		for (AuditLogEntry entry : auditLogEntries) {
			if (!entry.getTargetId().equals(ban.getUser().getId())) {
				continue;
			}

			handleBanLog(ban, entry);
			return;
		}

		handleBan(event.getGuild(), ban);
	}

	/**
	 * Handles and saves a ban where no audit log entry could be found, thus with an unknown actor.
	 *
	 * @param guild The guild the ban happened in.
	 * @param ban   The ban object containing the banned user and the reason of the ban.
	 */
	private void handleBan(final Guild guild, final Guild.Ban ban) {
		final Optional<DiscordGuild> dcGuildOpt = discordGuildRepo.findById(guild.getIdLong());
		if (dcGuildOpt.isEmpty()) {
			LogUtil.logWarning("Ignoring ban event on unknown guild \"" + guild.getName() + "\" (" + guild.getId() + ").");
			return;
		}

		final DiscordGuild dcGuild = dcGuildOpt.get();
		final User bannedUser = ban.getUser();
		final Optional<DiscordUser> bannedDiscordUserOpt = discordUserRepo.findById(bannedUser.getIdLong());
		bannedDiscordUserOpt.ifPresentOrElse(
				bannedDiscordUser -> saveBan(DiscordBan.withUnknownActor(ban.getReason(), dcGuild, bannedDiscordUser)),
				() -> {
					DiscordUser bannedDiscordUser = DiscordUser.createDiscordUser(bannedUser.getIdLong());
					discordUserRepo.save(bannedDiscordUser);
					saveBan(DiscordBan.withUnknownActor(ban.getReason(), dcGuild, bannedDiscordUser));
				}
		);

		LogUtil.logWarning("Received ban event for user \"" + bannedUser.getAsTag() + "\" (" +
				bannedUser.getId() + ") in guild \"" + guild.getName() + "\" (" + guild.getId() +
				") but could not find matching audit log entry!");

	}

	/**
	 * Handles and saves a ban with additional audit log information (actor).
	 *
	 * @param ban   The ban object containing the banned user and the reason of the ban.
	 * @param entry The audit log entry that matches the ban.
	 */
	private void handleBanLog(final Guild.Ban ban, final AuditLogEntry entry) {
		final Guild guild = entry.getGuild();
		final Optional<DiscordGuild> dcGuildOpt = discordGuildRepo.findById(guild.getIdLong());
		if (dcGuildOpt.isEmpty()) {
			LogUtil.logDebug("Ignoring ban event on unknown guild \"" + guild.getName() + "\" (" + guild.getId() + ").");
			return;
		}

		final User actor = entry.getUser();
		final long actorId = actor != null ? actor.getIdLong() : 0;
		if (actorId == 0) {
			LogUtil.logWarning("Received ban event for user (" + ban.getUser().getId() + ") but author is null!");
		}

		final DiscordGuild dcGuild = dcGuildOpt.get();
		final User bannedUser = ban.getUser();
		final String banReason = ban.getReason() == null ? (entry.getReason() == null ? "No reason given." : entry.getReason()) : ban.getReason();
		final Optional<DiscordUser> bannedDiscordUserOpt = discordUserRepo.findById(bannedUser.getIdLong());
		bannedDiscordUserOpt.ifPresentOrElse(
				bannedDiscordUser -> saveBan(DiscordBan.createDiscordBan(actorId, banReason, dcGuild, bannedDiscordUser)),
				() -> {
					DiscordUser bannedDiscordUser = DiscordUser.createDiscordUser(bannedUser.getIdLong());
					discordUserRepo.save(bannedDiscordUser);
					saveBan(DiscordBan.createDiscordBan(actorId, banReason, dcGuild, bannedDiscordUser));
				}
		);

		LogUtil.logDebug("\"" + ban.getUser().getAsTag() + "\" (" + ban.getUser().getId() +
				") got banned for \"" + banReason + "\"");
	}

	/**
	 * Saves the ban in the database.
	 *
	 * @param discordBan The dao ban object.
	 */
	private void saveBan(final DiscordBan discordBan) {
		discordBanRepo.save(discordBan);
	}

	/**
	 * Handles a guild unban event and deletes any matching Discord ban present in the database.
	 *
	 * @param event The unban event triggered by Discord.
	 */
	@Override
	public void onGuildUnban(final GuildUnbanEvent event) {
		final Guild guild = event.getGuild();
		final User unbannedUser = event.getUser();

		final Optional<DiscordBan> banOpt = discordBanRepo.findDiscordBanByBannedUser_DiscordIdAndGuild_GuildId(unbannedUser.getIdLong(), guild.getIdLong());
		banOpt.ifPresentOrElse(
				ban -> removeBan(event, ban),
				() -> LogUtil.logWarning("\"" + unbannedUser.getAsTag() + "\" got unbanned on \"" +
						guild.getName() + "\" (" + guild.getId() + ") but no database entry could be found!")
		);
	}

	/**
	 * Retrieves all unban audit logs to check for further information about the unban.
	 *
	 * @param event The unban event triggered by Discord.
	 * @param ban   The Discord ban information saved in the database.
	 */
	private void removeBan(final GuildUnbanEvent event, final DiscordBan ban) {
		event.getGuild()
				.retrieveAuditLogs()
				.type(ActionType.UNBAN)            // only keep audit logs about unbans
				.queueAfter(15, TimeUnit.SECONDS, logEntries -> findUnban(event, ban, logEntries));
	}

	/**
	 * Matches all unban audit logs to the unban information, if there is a match the actor is known and can be logged.
	 *
	 * @param event           The unban event triggered by Discord.
	 * @param ban             The Discord ban information saved in the database.
	 * @param auditLogEntries The list of audit log entries.
	 */
	private void findUnban(final GuildUnbanEvent event, final DiscordBan ban, final List<AuditLogEntry> auditLogEntries) {
		if (auditLogEntries.size() == 0) {
			handleMissingAuditLog(event, ban);
			return;
		}

		for (AuditLogEntry entry : auditLogEntries) {
			if (entry.getTargetIdLong() != ban.getBannedUser().getDiscordId()) {
				continue;
			}

			deleteBanEntry(ban, entry);
			return;
		}

		handleMissingAuditLog(event, ban);
	}

	/**
	 * Deletes ban information in the database without information about the actor.
	 *
	 * @param event The unban event triggered by Discord.
	 * @param ban   The Discord ban information saved in the database.
	 */
	private void handleMissingAuditLog(final GuildUnbanEvent event, final DiscordBan ban) {
		final Guild guild = event.getGuild();
		discordBanRepo.deleteById(ban.getBanId());
		LogUtil.logWarning("Received unban event for user ID " + ban.getBannedUser().getDiscordId() + " in guild \"" +
				guild.getName() + "\" (" + guild.getId() + ") but could not find matching audit log entry!");
	}

	/**
	 * Deletes ban information in the database and logs information about the actor if present.
	 *
	 * @param ban   The Discord ban information saved in the database.
	 * @param entry The audit log entry that matches the unban event.
	 */
	private void deleteBanEntry(final DiscordBan ban, final AuditLogEntry entry) {
		final Guild guild = entry.getGuild();
		final String authorTag = entry.getUser() != null ? entry.getUser().getAsTag() : "UnknownUser";
		final String authorId = entry.getUser() != null ? entry.getUser().getId() : "0";
		discordBanRepo.deleteById(ban.getBanId());
		LogUtil.logDebug(ban.getBannedUser().getDiscordId() + " got unbanned on \"" + guild.getName() + "\" (" + guild.getId() +
				") by \"" + authorTag + "\" (" + authorId + ").");
	}
}
