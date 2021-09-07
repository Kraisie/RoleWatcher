package com.motorbesitzen.rolewatcher.bot.event;

import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.bot.service.ForumRoleApiRequest;
import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordBanRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.RoleUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

/**
 * Handles member guild joins. Assigns the fitting roles if the Discord user is linked to a forum user.
 */
@Service
public class GuildMemberJoinListener extends ListenerAdapter {

	private final EnvSettings envSettings;
	private final ForumUserRepo forumUserRepo;
	private final ForumRoleRepo forumRoleRepo;
	private final DiscordBanRepo discordBanRepo;
	private final ForumRoleApiRequest forumRoleApiRequest;

	@Autowired
	public GuildMemberJoinListener(final EnvSettings envSettings, final ForumUserRepo forumUserRepo,
								   final ForumRoleRepo forumRoleRepo, final DiscordBanRepo discordBanRepo,
								   final ForumRoleApiRequest forumRoleApiRequest) {
		this.envSettings = envSettings;
		this.forumUserRepo = forumUserRepo;
		this.forumRoleRepo = forumRoleRepo;
		this.discordBanRepo = discordBanRepo;
		this.forumRoleApiRequest = forumRoleApiRequest;
	}

	/**
	 * If a member joins a guild with the bot in it the bot assigns roles to the user if the user is linked.
	 *
	 * @param event The Discord event that a member joined a guild.
	 */
	@Override
	public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
		final Guild guild = event.getGuild();
		final Member member = event.getMember();

		final Optional<DiscordBan> banOpt = discordBanRepo.findDiscordBanByBannedUser_DiscordIdAndGuild_GuildId(member.getIdLong(), guild.getIdLong());
		banOpt.ifPresentOrElse(
				ban -> member.ban(0, "User found on ban list. Reason: " + ban.getReason()).queue(),
				() -> {
					Optional<ForumUser> forumUserOpt = forumUserRepo.findByLinkedDiscordUser_DiscordId(member.getIdLong());
					forumUserOpt.ifPresent(forumUser -> assignForumRoles(member, forumUser));
				}
		);
	}

	/**
	 * Assigns the forum roles to the member. If the user has the banned role on the forum the bot bans the member.
	 *
	 * @param member    The member who joined the guild.
	 * @param forumUser The information about the matching forum user in the database.
	 */
	private void assignForumRoles(final Member member, final ForumUser forumUser) {
		final List<ForumRole> forumRoles;
		try {
			forumRoles = forumRoleApiRequest.getRolesOfForumUser(forumUser);
		} catch (SocketTimeoutException e) {
			LogUtil.logError("Skipping user due to API timeout. Could not get roles of " + forumUser);
			return;
		} catch (IOException | IllegalArgumentException e) {
			LogUtil.logError("Skipping user. Could not get roles of " + forumUser, e);
			return;
		}

		if (RoleUtil.hasBannedRole(envSettings, forumRoles)) {
			member.ban(0, "User (" + forumUser.getForumId() + ") has the banned role on the forum. Might be a temporary ban.").queue();
			return;
		}

		RoleUtil.updateRoles(member, forumRoles, forumRoleRepo.findAll());
	}
}
