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
	public GuildJoinListener(DiscordGuildRepo discordGuildRepo, DiscordBanRepo discordBanRepo, DiscordUserRepo discordUserRepo) {
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
	 *
	 * @param guild   The guild the bot got added to.
	 * @param dcGuild The database representation of the guild.
	 * @param bans    The list of bans of the guild.
	 */
	private void handleBanList(final Guild guild, final DiscordGuild dcGuild, final List<Guild.Ban> bans) {
		for (Guild.Ban ban : bans) {
			guild.retrieveAuditLogs()
					.type(ActionType.BAN)
					.forEachAsync(entry -> findBan(dcGuild, ban, entry));
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
	 * {@code false}
	 */
	private boolean findBan(final DiscordGuild dcGuild, final Guild.Ban ban, final AuditLogEntry entry) {
		if (entry.getTargetId().equals(ban.getUser().getId())) {
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
		User actor = entry.getUser();
		User bannedUser = ban.getUser();
		long bannedUserId = bannedUser.getIdLong();
		long actorId = actor == null ? 0 : actor.getIdLong();
		String reason = entry.getReason() == null ? "No reason given." : entry.getReason();

		if (discordBanRepo.existsByBannedUser_DiscordIdAndGuild_GuildId(bannedUserId, dcGuild.getGuildId())) {
			// if user got banned multiple times and the latest is already in the database skip older
			// bans as the audit log is ordered by new to old.
			return;
		}

		Optional<DiscordUser> dcBannedUserOpt = discordUserRepo.findById(bannedUserId);
		dcBannedUserOpt.ifPresentOrElse(
				dcBannedUser -> discordBanRepo.save(DiscordBan.createDiscordBan(actorId, reason, dcGuild, dcBannedUser)),
				() -> {
					DiscordUser dcUser = DiscordUser.createDiscordUser(bannedUserId);
					discordUserRepo.save(dcUser);
					discordBanRepo.save(DiscordBan.createDiscordBan(actorId, reason, dcGuild, dcUser));
				}
		);
	}
}