package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordBanRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.RoleUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Deletes a forum user entry and by that the link between Discord and forum user from the database.
 */
@Service("deluser")
class DeleteUser extends CommandImpl {

	private final ForumRoleRepo forumRoleRepo;
	private final ForumUserRepo forumUserRepo;
	private final DiscordBanRepo banRepo;
	private final EnvSettings envSettings;

	@Autowired
	private DeleteUser(final ForumRoleRepo forumRoleRepo, final ForumUserRepo forumUserRepo,
					   final DiscordBanRepo banRepo, final EnvSettings envSettings) {
		this.forumRoleRepo = forumRoleRepo;
		this.forumUserRepo = forumUserRepo;
		this.banRepo = banRepo;
		this.envSettings = envSettings;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "deluser";
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
		return getName() + " (@member|discordid|uid)";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Removes a link between Discord and forum user. Can be used with a Discord tag, " +
				"a Discord ID or a forum ID.";
	}

	/**
	 * Deletes a forum user from the database by its forum or Discord ID. If there is no ID found in the message then
	 * an error message gets send in the channel where the command got used. If the user ID is not found in the
	 * database it also responds with an error message.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final long memberId = DiscordMessageUtil.getMentionedMemberId(event.getMessage());
		if (memberId == -1) {
			sendErrorMessage(channel, "Please provide a mention or an ID for the delete command.");
			return;
		}

		final Optional<ForumUser> forumUser = forumUserRepo.findByForumIdOrLinkedDiscordUser_DiscordId(memberId, memberId);
		forumUser.ifPresentOrElse(user -> tryDeleteUser(event, user), () -> sendErrorMessage(channel, "User not found! Make sure the given ID exists."));
	}

	/**
	 * Tries to delete the user from the database and logs the delete action. Sends a success message after deletion.
	 * Does not unlink the user if the user is banned in the guild. Informs caller about the ban and
	 * explains what to do to unlink the user.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The user to delete from the database.
	 */
	private void tryDeleteUser(final GuildMessageReceivedEvent event, final ForumUser user) {
		if (event.getMessage().getContentRaw().endsWith("-f")) {
			deleteUser(event, user);
			return;
		}

		final long discordId = user.getLinkedDiscordUser().getDiscordId();
		final Optional<DiscordBan> dcBanOpt = banRepo.findByBannedUser_DiscordId(discordId);
		dcBanOpt.ifPresentOrElse(
				dcBan -> informOfUserBan(event.getChannel(), dcBan.getReason(), user),
				() -> event.getGuild().retrieveBanById(discordId).queue(
						ban -> informOfUserBan(event.getChannel(), ban.getReason(), user),
						throwable -> deleteUser(event, user)
				)
		);
	}

	/**
	 * Informs about a ban for the user that is supposed to get unlinked.
	 *
	 * @param channel   The channel to send the message in.
	 * @param banReason The reason for the ban of the user if given.
	 */
	private void informOfUserBan(final TextChannel channel, String banReason, final ForumUser user) {
		if (banReason == null) {
			banReason = "Unknown reason (not set in API and database)";
		} else if (banReason.isBlank()) {
			banReason = "No reason given.";
		}

		sendTextMessageWithEmbed(
				channel,
				"The user you tried to unlink is banned for \"" + banReason + "\" on this guild!\n" +
						"Unban the user and try again if you still want to unlink the user. If the user is " +
						"**already unbanned** or the **ban is imported** try to add \"-f\" to the end of the message " +
						"(without the quotation marks).",
				buildUserInfo(user, user.getLinkedDiscordUser())
		);
	}

	/**
	 * Creates an embed with the information about the user.
	 *
	 * @param user   The forum user to show the info of.
	 * @param dcUser The Discord user who was/is linked to the forum user as the user might got unlinked already.
	 * @return The MessageEmbed with the information.
	 */
	private MessageEmbed buildUserInfo(final ForumUser user, final DiscordUser dcUser) {
		return new EmbedBuilder()
				.setTitle("User information:")
				.setColor(envSettings.getEmbedColor())
				.addField("Discord user:", "<@" + dcUser.getDiscordId() + ">", true)
				.addField("Whitelisted?", (dcUser.isWhitelisted() ? "Yes" : "No"), true)
				.addBlankField(false)
				.addField("Forum User:", user.getForumUsername() + " (UID: " + user.getForumId() + ")", false)
				.addField("Link: ", envSettings.getForumMemberProfileUrl() + user.getForumId(), false)
				.build();
	}

	/**
	 * Deletes the user from the database and logs the delete action. Sends a success message after deletion.
	 * Does not remove Discord roles of the deleted user if the user is whitelisted.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The user to delete from the database.
	 */
	private void deleteUser(final GuildMessageReceivedEvent event, final ForumUser user) {
		final DiscordUser dcUser = user.getLinkedDiscordUser();
		user.setLinkedDiscordUser(null);
		forumUserRepo.save(user);    // unlinking from discord user, otherwise won't delete entry
		forumUserRepo.delete(user);
		sendTextMessageWithEmbed(event.getChannel(), "Deleted the following user from the database.", buildUserInfo(user, dcUser));

		if (!dcUser.isWhitelisted()) {
			removeForumRoles(event.getGuild(), dcUser.getDiscordId());
		}
	}

	/**
	 * Removes the forum roles of the user on the guild where the command got triggered.
	 * Every other guild will only update via RoleUpdater.
	 *
	 * @param guild The guild the user is in.
	 */
	private void removeForumRoles(final Guild guild, final long discordId) {
		guild.retrieveMemberById(discordId).queue(
				member -> RoleUtil.updateRoles(member, new ArrayList<>(), forumRoleRepo.findAll()),
				throwable -> LogUtil.logDebug("Can not remove roles from deleted user as user is not in the guild.")
		);
	}
}
