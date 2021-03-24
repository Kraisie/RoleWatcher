package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Deletes a forum role from the database.
 */
@Service("delrole")
class DeleteRole extends CommandImpl {

	private final ForumRoleRepo forumRoleRepo;

	@Autowired
	private DeleteRole(final ForumRoleRepo forumRoleRepo) {
		this.forumRoleRepo = forumRoleRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "delrole";
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
		return getName() + " id";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Deletes a forum role from the database.";
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
