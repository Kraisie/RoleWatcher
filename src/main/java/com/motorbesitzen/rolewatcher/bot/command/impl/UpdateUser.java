package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.bot.service.ForumRoleApiRequest;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.RoleUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Manually force a role update of a Discord member.
 */
@Service("update")
class UpdateUser extends CommandImpl {

	private final EnvSettings envSettings;
	private final ForumUserRepo forumUserRepo;
	private final ForumRoleRepo forumRoleRepo;
	private final ForumRoleApiRequest forumRoleApiRequest;

	@Autowired
	private UpdateUser(final EnvSettings envSettings, final ForumUserRepo forumUserRepo,
					   final ForumRoleRepo forumRoleRepo, final ForumRoleApiRequest forumRoleApiRequest) {
		this.envSettings = envSettings;
		this.forumUserRepo = forumUserRepo;
		this.forumRoleRepo = forumRoleRepo;
		this.forumRoleApiRequest = forumRoleApiRequest;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "update";
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
		// the guild permissions do not matter as only the owner can use it
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
		return getName() + " (@member|discordid|uid)";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Forces an update of the roles of the user according to his forum roles.";
	}

	/**
	 * Updates the Discord roles of the mentioned member by calling the forum API.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final TextChannel channel = event.getChannel();
		final long mentionedId = DiscordMessageUtil.getMentionedMemberId(message);
		if (mentionedId == -1) {
			sendErrorMessage(channel, "Please provide a mention or an ID for the add command.");
			return;
		}

		final Optional<ForumUser> forumUserOpt = forumUserRepo.findByForumIdOrLinkedDiscordUser_DiscordId(mentionedId, mentionedId);
		forumUserOpt.ifPresentOrElse(
				forumUser -> updateRoles(channel, forumUser),
				() -> sendErrorMessage(channel, "No user found with that ID.")
		);
	}

	/**
	 * Updates the roles of a Discord user if the user is a member of the caller guild.
	 *
	 * @param channel   The channel the command got triggered in.
	 * @param forumUser The user with the given ID in the database.
	 */
	private void updateRoles(final TextChannel channel, final ForumUser forumUser) {
		final Guild guild = channel.getGuild();
		guild.retrieveMemberById(forumUser.getLinkedDiscordUser().getDiscordId()).queue(
				member -> updateMemberRoles(channel, forumUser, member),
				throwable -> sendErrorMessage(channel, "Member not found, make sure the user is in your guild!")
		);
	}

	/**
	 * Requests the current forum roles of the user from the forum API and updates the roles of the Discord user
	 * accordingly. If the user has the banned role on the forum he will get banned by the bot.
	 *
	 * @param channel   The channel the command got triggered in.
	 * @param forumUser The user with the given ID in the database.
	 * @param member    The matching member of the {@param forumUser}.
	 */
	private void updateMemberRoles(final TextChannel channel, final ForumUser forumUser, final Member member) {
		final List<ForumRole> forumRoles;
		try {
			forumRoles = forumRoleApiRequest.getRolesOfForumUser(forumUser);
		} catch (IOException e) {
			sendErrorMessage(channel, "Could not get roles of user!");
			LogUtil.logError("Could not get roles of " + forumUser.toString(), e);
			return;
		}

		if (RoleUtil.hasBannedRole(envSettings, forumRoles)) {
			member.ban(0, "User (" + forumUser.getForumId() + ") has the banned role on the forum. Might be a temporary ban.").queue();
			sendErrorMessage(channel, "Member has the banned role on the forum and thus has been banned.");
			return;
		}

		RoleUtil.updateRoles(member, forumRoles, forumRoleRepo.findAll());
		answer(channel, "Updated roles of the mentioned user.");
	}


}
