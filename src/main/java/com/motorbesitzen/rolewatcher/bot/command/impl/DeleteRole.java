package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * Deletes a forum role from the database.
 */
public class DeleteRole extends CommandImpl {

	private final ForumRoleRepo forumRoleRepo;

	public DeleteRole(final ForumRoleRepo forumRoleRepo) {
		this.forumRoleRepo = forumRoleRepo;
	}

	/**
	 * Deletes a forum role from the database. Sends an error message iff no role ID is not given.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final long roleId = DiscordMessageUtil.getMentionedRawId(event.getMessage());
		if (roleId == -1) {
			sendErrorMessage(event.getChannel(), "Please provide a role ID to delete!");
			return;
		}

		if (!forumRoleRepo.existsById(roleId)) {
			sendErrorMessage(event.getChannel(), "There is no role with that ID!");
			return;
		}

		forumRoleRepo.deleteById(roleId);
		answer(event.getChannel(), "Deleted role from database.");
	}
}
