package com.motorbesitzen.rolewatcher.bot.service;

import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.ParseUtil;
import com.motorbesitzen.rolewatcher.util.RoleUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Updates the roles of all members of all guilds the bot is in (if not whitelisted and if the guild has the needed permission).
 */
@Service
public class RoleUpdater {

	private final JDA jda;
	private final EnvSettings envSettings;
	private final DiscordUserRepo discordUserRepo;
	private final ForumUserRepo forumUserRepo;
	private final ForumRoleRepo forumRoleRepo;
	private final DiscordGuildRepo guildRepo;
	private final ScheduledExecutorService scheduler;
	private final ForumRoleApiRequest apiRequest;
	private final int delayMs;

	@Autowired
	private RoleUpdater(final JDA jda, final EnvSettings envSettings, final DiscordUserRepo discordUserRepo,
						final ForumUserRepo forumUserRepo, final ForumRoleRepo forumRoleRepo,
						final DiscordGuildRepo guildRepo, final ForumRoleApiRequest apiRequest) {
		this.jda = jda;
		this.envSettings = envSettings;
		this.discordUserRepo = discordUserRepo;
		this.forumUserRepo = forumUserRepo;
		this.forumRoleRepo = forumRoleRepo;
		this.guildRepo = guildRepo;
		this.scheduler = Executors.newScheduledThreadPool(3);
		this.apiRequest = apiRequest;
		this.delayMs = getDelay();
	}

	/**
	 * Gets the set delay or uses the default of 5000ms if none is set. If the delay is below 100ms
	 * it gets set to 100ms to prevent too many requests. If anything below 100ms is needed one should probably
	 * switch to batch requests.
	 *
	 * @return The delay between each member in milliseconds.
	 */
	private int getDelay() {
		final String delayStr = envSettings.getForumRoleApiDelay();
		return Math.max(100, ParseUtil.safelyParseStringToInt(delayStr));
	}

	/**
	 * Starts the role updater.
	 */
	public void start() {
		LogUtil.logDebug("Starting scheduled user updates...");
		run();
	}

	/**
	 * Schedules the role update for all members of all guilds the bot is in.
	 */
	private void run() {
		try {
			LogUtil.logDebug("Running user updates");
			doRoleUpdates();
		} catch (Exception e) {
			LogUtil.logWarning("Unexpected Exception: " + e);
		} catch (Throwable t) {
			LogUtil.logWarning("Unexpected Error: " + t);
		}
	}

	/**
	 * Schedules all guilds by settings the delay between guilds based on the member count and the set base delay.
	 * If there are no guilds the bot is a member of or there are no members increase the delay to 60 seconds
	 * until the next run.
	 */
	private void doRoleUpdates() {
		final List<Guild> guilds = jda.getGuilds();
		int memberQueueCount = 0;
		for (Guild guild : guilds) {
			if (!hasRoleSyncPerms(guild)) {
				continue;
			}

			final long guildDelay = (long) memberQueueCount * delayMs + delayMs;
			LogUtil.logDebug("Scheduling guild \"" + guild.getName() + "\"... (" + guild.getMemberCount() + " -> " + guildDelay + ")");
			scheduler.schedule(updateGuildMembers(guild), guildDelay, TimeUnit.MILLISECONDS);
			memberQueueCount += guild.getMemberCount();
		}

		long nextRunDelay = (long) memberQueueCount * delayMs + delayMs;
		if (nextRunDelay == delayMs) {
			nextRunDelay = 60000;
		}

		scheduler.schedule(this::run, nextRunDelay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Checks if a guild has the needed permission to sync roles between Discord and forum.
	 *
	 * @param guild The guild.
	 * @return {@code true} if it has the needed permission.
	 */
	private boolean hasRoleSyncPerms(final Guild guild) {
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		if (dcGuildOpt.isEmpty()) {
			return false;
		}

		return dcGuildOpt.get().hasRoleSyncPerm();
	}

	/**
	 * Schedules all members of a guild for their role update.
	 *
	 * @param guild The guild to update the member roles of.
	 * @return A {@code Runnable} for the summarised task.
	 */
	private Runnable updateGuildMembers(final Guild guild) {
		LogUtil.logDebug("Updating guild \"" + guild.getName() + "\"...");
		return () -> guild.loadMembers().onSuccess(members -> scheduler.execute(() -> {
			for (int i = 0; i < members.size(); i++) {
				LogUtil.logDebug("Scheduling member \"" + members.get(i).getUser().getAsTag() + "\" (" + members.get(i).getId() + ") on \"" + guild.getName() + "\"... (" + (i + 1) + "/" + guild.getMemberCount() + " -> " + (i * delayMs) + ")");
				scheduler.schedule(updateMember(members.get(i)), (long) i * delayMs, TimeUnit.MILLISECONDS);
			}
		}));
	}

	/**
	 * Updates the roles of a member.
	 *
	 * @param member The member to update the roles of.
	 * @return A {@code Runnable} for the summarised task.
	 */
	private Runnable updateMember(final Member member) {
		return () -> {
			LogUtil.logDebug("Updating member \"" + member.getUser().getAsTag() + "\" (" + member.getId() + ") on \"" + member.getGuild().getName() + "\"...");
			final long discordId = member.getIdLong();
			final Optional<ForumUser> forumUserOpt = forumUserRepo.findByLinkedDiscordUser_DiscordId(discordId);
			forumUserOpt.ifPresentOrElse(
					forumUser -> updateMemberRoles(forumUser, member),
					() -> checkForKick(member)
			);
		};
	}

	/**
	 * Update the roles of the member and bans the member if it has the banned role on the forum.
	 *
	 * @param forumUser The matching forum user to the member.
	 * @param member    The member to update the roles of.
	 */
	private void updateMemberRoles(final ForumUser forumUser, final Member member) {
		if (forumUser.getLinkedDiscordUser().isWhitelisted()) {
			return;
		}

		final List<ForumRole> forumRoles;
		try {
			forumRoles = apiRequest.getRolesOfForumUser(forumUser);
		} catch (SocketTimeoutException e) {
			LogUtil.logError("Skipping user due to API timeout. Could not get roles of " + forumUser);
			return;
		} catch (IOException e) {
			LogUtil.logError("Skipping user. Could not get roles of " + forumUser, e);
			return;
		}

		if (RoleUtil.hasBannedRole(envSettings, forumRoles)) {
			banMember(forumUser, member);
			return;
		}

		updateRoles(member, forumRoles);
	}

	/**
	 * Bans the member if possible and logs the result.
	 *
	 * @param forumUser The matching forum user to the member.
	 * @param member    The member to ban.
	 */
	private void banMember(final ForumUser forumUser, final Member member) {
		member.ban(0, "User (" + forumUser.getForumId() + ") has the banned role on the forum. Might be a temporary ban.").queue(
				(ban) -> LogUtil.logInfo(
						"Banned member " + member.getUser().getAsTag() + " (" + member.getId() + ")  from \"" +
								member.getGuild().getName() + "\" (" + member.getGuild().getId() + ") " +
								"due to having the banned role. User is linked to " + forumUser + "."
				),
				throwable -> LogUtil.logError("Could not ban " + forumUser + " on \"" + member.getGuild().getName() + "\".", throwable)
		);
	}

	/**
	 * Updates the roles of the member if possible.
	 *
	 * @param member     The member to update the roles of.
	 * @param forumRoles The list of roles the member has on the forum.
	 */
	private void updateRoles(final Member member, final List<ForumRole> forumRoles) {
		try {
			RoleUtil.updateRoles(member, forumRoles, forumRoleRepo.findAll());
		} catch (InsufficientPermissionException e) {
			LogUtil.logWarning("Bot does not have the needed permission " + e.getPermission() + " in \"" + member.getGuild().getName() + "\" to update roles.");
		} catch (HierarchyException e) {
			LogUtil.logWarning("Bot can not modify some forum roles in \"" + member.getGuild().getName() + "\"! Please move the bot role above any forum role.");
		}
	}

	/**
	 * Checks if the user should be kicked and kicks the user accordingly.
	 *
	 * @param member The member to check.
	 */
	private void checkForKick(final Member member) {
		if (shouldKick(member)) {
			final Duration memberTime = Duration.between(member.getTimeJoined(), OffsetDateTime.now());
			final long days = memberTime.toDays();
			final long hours = memberTime.toHoursPart();
			final long minutes = memberTime.toMinutesPart();
			final String joinedText =
					"Joined " +
							(days != 0 ? "**" + days + "**d " : "") +
							(hours != 0 ? "**" + hours + "**h " : "") +
							(minutes != 0 ? "**" + minutes + "**m " : "") +
							"ago";

			member.kick("Autokick due to being unlinked. (" + joinedText + ")").queue();
			LogUtil.logInfo(
					"Kicked member " + member.getUser().getAsTag() + " (" + member.getId() + ") from " +
							"\"" + member.getGuild().getName() + "\" (" + member.getGuild().getId() + ") due to being unlinked."
			);
		}
	}

	/**
	 * Determines if a member should be kicked automatically.
	 *
	 * @param member The member to check.
	 * @return {@code true} if the guild has autokick permissions, the member isn't a bot or the owner and if the user
	 * joined x hours ago, {@code false} otherwise.
	 */
	private boolean shouldKick(final Member member) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(member.getGuild().getIdLong());
		if (dcGuildOpt.isEmpty()) {
			return false;
		}

		final DiscordGuild dcGuild = dcGuildOpt.get();
		if (!dcGuild.canAutokick()) {
			return false;
		}

		final Optional<DiscordUser> dcUserOpt = discordUserRepo.findById(member.getIdLong());
		if (dcUserOpt.isPresent()) {
			if (dcUserOpt.get().isWhitelisted()) {
				return false;
			}
		}

		if (member.getUser().isBot()) {
			return false;
		}

		if (member.isOwner()) {
			return false;
		}

		return member.getTimeJoined().isBefore(OffsetDateTime.now().minusHours(dcGuild.getAutokickHourDelay()));
	}
}
