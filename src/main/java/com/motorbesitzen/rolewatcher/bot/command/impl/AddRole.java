package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Add a forum role to the database.
 */
@Service("addrole")
public class AddRole extends CommandImpl {

	private final ForumRoleRepo forumRoleRepo;

	@Autowired
	public AddRole(final ForumRoleRepo forumRoleRepo) {
		this.forumRoleRepo = forumRoleRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "addrole";
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
		return getName() + " id \"rolename\"";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Adds a forum role to the database.";
	}

	/**
	 * Adds a forum role to the database. Sends an error message if role ID or role name is not given.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final long roleId = DiscordMessageUtil.getMentionedRawId(message);
		if (roleId == -1) {
			sendErrorMessage(event.getChannel(), "Please provide a role ID!");
			return;
		}

		if (forumRoleRepo.existsById(roleId)) {
			sendErrorMessage(event.getChannel(), "Role with that ID already exists!");
			return;
		}

		final Optional<String> roleNameOpt = getRoleName(message.getContentRaw());
		roleNameOpt.ifPresentOrElse(
				roleName -> saveRole(event, roleId, roleName),
				() -> sendErrorMessage(event.getChannel(), "Please provide a role name!")
		);
	}

	/**
	 * Gets the role name from the raw command message.
	 *
	 * @param rawMessage The raw text of the command.
	 * @return The name of the role if it is given, {@code Optional.empty()} if not.
	 */
	private Optional<String> getRoleName(final String rawMessage) {
		if (!rawMessage.matches(".*\".*\".*")) {
			return Optional.empty();
		}

		return Optional.of(rawMessage.substring(rawMessage.indexOf('"') + 1, rawMessage.lastIndexOf('"')));
	}

	/**
	 * Saves a role to the database.
	 *
	 * @param event    The Discord event triggered when a message is received.
	 * @param roleId   The role ID.
	 * @param roleName The role name.
	 */
	private void saveRole(final GuildMessageReceivedEvent event, final long roleId, final String roleName) {
		final ForumRole forumRole = ForumRole.of(roleId, roleName);
		forumRoleRepo.save(forumRole);
		answer(event.getChannel(), "Added role to database.");
	}
}
