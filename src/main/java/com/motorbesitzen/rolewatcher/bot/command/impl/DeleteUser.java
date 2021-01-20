package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.RoleUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Deletes a forum user entry and by that the link between Discord and forum user from the database.
 */
public class DeleteUser extends CommandImpl {

	private final ForumRoleRepo forumRoleRepo;
	private final ForumUserRepo forumUserRepo;

	public DeleteUser(final ForumRoleRepo forumRoleRepo, final ForumUserRepo forumUserRepo) {
		this.forumRoleRepo = forumRoleRepo;
		this.forumUserRepo = forumUserRepo;
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

		Optional<ForumUser> forumUser = forumUserRepo.findByForumIdOrLinkedDiscordUser_DiscordId(memberId, memberId);
		forumUser.ifPresentOrElse(user -> deleteUser(event, user), () -> sendErrorMessage(channel, "User not found! Make sure the given ID exists."));
	}

	/**
	 * Deletes the user from the database and logs the delete action. Sends a success message after deletion.
	 * Does not remove Discord roles of the deleted user if the user is whitelisted. Informs caller if the user
	 * to unlink is banned on this guild.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The user to delete from the database.
	 */
	private void deleteUser(final GuildMessageReceivedEvent event, final ForumUser user) {
		// saving needed data, because hibernate nulls the user after deletion from the database
		long discordId = user.getLinkedDiscordUser().getDiscordId();
		boolean whitelisted = user.getLinkedDiscordUser().isWhitelisted();

		event.getGuild().retrieveBanById(discordId).queue(
				ban -> {
					final String banReason = ban.getReason();
					sendMessage(event.getChannel(), "The user you are unlinking is banned for \"" + banReason + "\" on this guild!");
				}
		);

		user.setLinkedDiscordUser(null);
		forumUserRepo.save(user);    // unlinking from discord user, otherwise won't delete entry
		forumUserRepo.delete(user);
		answer(event.getChannel(), "Deleted user from database.");
		doUserDeletionLog(event, discordId, user);

		if (whitelisted) {
			return;
		}
		removeForumRoles(event.getGuild(), discordId);
	}

	/**
	 * Removes the forum roles of the user on the guild where the command got triggered.
	 * Every other guild will only update via RoleUpdater.
	 *
	 * @param guild The guild the user is in.
	 */
	private void removeForumRoles(final Guild guild, final long discordId) {
		guild.retrieveMemberById(discordId).queue(
				member -> RoleUtil.updateRoles(member, new ArrayList<>(), forumRoleRepo.findAll())
		);
	}

	/**
	 * Logs the delete action.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The deleted user.
	 */
	private void doUserDeletionLog(final GuildMessageReceivedEvent event, final long discordId, final ForumUser user) {
		final String authorId = event.getAuthor().getId();
		final String author = event.getAuthor().getAsTag();
		final String message = author + " (" + authorId + ")" + " deleted a user (" + discordId + ") from the database: " + user.toString() + ".";
		LogUtil.logInfo(message);
	}
}
