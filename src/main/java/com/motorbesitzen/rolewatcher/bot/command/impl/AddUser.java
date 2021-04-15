package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.bot.service.ForumRoleApiRequest;
import com.motorbesitzen.rolewatcher.bot.service.RoleUpdater;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
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
 * A command to manually add a link between a Discord and a forum user.
 */
@Service("adduser")
class AddUser extends CommandImpl {

	private final EnvSettings envSettings;
	private final DiscordUserRepo discordUserRepo;
	private final ForumUserRepo forumUserRepo;
	private final ForumRoleRepo forumRoleRepo;
	private final ForumRoleApiRequest forumRoleApiRequest;

	@Autowired
	private AddUser(final EnvSettings envSettings, final DiscordUserRepo discordUserRepo,
					final ForumUserRepo forumUserRepo, final ForumRoleRepo forumRoleRepo,
					final ForumRoleApiRequest forumRoleApiRequest) {
		this.envSettings = envSettings;
		this.discordUserRepo = discordUserRepo;
		this.forumUserRepo = forumUserRepo;
		this.forumRoleRepo = forumRoleRepo;
		this.forumRoleApiRequest = forumRoleApiRequest;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "adduser";
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
		return getName() + " @member \"username\" uid";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Adds a link between a Discord and a forum user.";
	}

	/**
	 * Executes the command by getting all needed information from the command parameters and adds a link between
	 * a Discord and a forum user to the database with the given information. Sends error messages to the channel
	 * where the command got used if any information is missing or invalid or if the mentioned Discord or forum
	 * user is already linked to another user. Assigns the forum roles the user has to the Discord member on
	 * success and logs the action.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final TextChannel channel = event.getChannel();
		final long discordId = DiscordMessageUtil.getMentionedMemberId(message);
		if (discordId <= 10000000000000000L) {
			sendErrorMessage(channel, "Please provide a mention or an ID for the add command.");
			return;
		}

		final String rawMessage = message.getContentRaw();
		final Optional<String> nameOpt = getUserName(rawMessage);
		if (nameOpt.isEmpty()) {
			sendErrorMessage(channel, "Please provide a username in quotation marks (\"...\").");
			return;
		}

		final String[] tokens = rawMessage.split(" ");
		final String strUserId = tokens[tokens.length - 1];
		if (!strUserId.matches("[0-9]+")) {
			sendErrorMessage(channel, "Please provide a user forum ID at the end of the message.");
			return;
		}

		final long userId = Long.parseLong(strUserId);
		final Optional<ForumUser> forumUserByUidOpt = forumUserRepo.findById(userId);
		if (forumUserByUidOpt.isPresent()) {
			ForumUser user = forumUserByUidOpt.get();
			sendErrorMessage(channel, "Forum user \"" + user.getForumUsername() + "\" (UID: " + user.getForumId() +
					") is already linked to <@" + user.getLinkedDiscordUser().getDiscordId() + ">.");
			return;
		}

		final Optional<ForumUser> forumUserByDcIdOpt = forumUserRepo.findByLinkedDiscordUser_DiscordId(discordId);
		if (forumUserByDcIdOpt.isPresent()) {
			ForumUser user = forumUserByDcIdOpt.get();
			sendErrorMessage(channel, "That discord user (" + user.getLinkedDiscordUser().getDiscordId() + ") " +
					"is already linked to \"" + user.getForumUsername() + "\" (UID: " + user.getForumId() + ").");
			return;
		}

		final ForumUser newForumUser = ForumUser.create(userId, nameOpt.get());
		final Optional<DiscordUser> dcUserOpt = discordUserRepo.findById(discordId);
		dcUserOpt.ifPresentOrElse(dcUser -> addForumUserLink(dcUser, newForumUser), () -> createDiscordUserLink(discordId, newForumUser));

		assignUserRoles(channel, newForumUser);
		doUserAddLog(event, newForumUser);
	}

	/**
	 * Add a link between a forum user and an existing Discord user in the database.
	 *
	 * @param dcUser    The existing Discord user.
	 * @param forumUser The forum user to add to the Discord user.
	 */
	private void addForumUserLink(final DiscordUser dcUser, final ForumUser forumUser) {
		forumUser.setLinkedDiscordUser(dcUser);
		dcUser.setLinkedForumUser(forumUser);
		discordUserRepo.save(dcUser);
	}

	/**
	 * Create a new Discord user and link it to the given forum user.
	 *
	 * @param discordId The Discord ID of the Discord user.
	 * @param forumUser The forum user to add to the Discord user.
	 */
	private void createDiscordUserLink(final long discordId, final ForumUser forumUser) {
		final DiscordUser newDcUser = DiscordUser.createLinkedDiscordUser(discordId, forumUser);
		forumUser.setLinkedDiscordUser(newDcUser);
		discordUserRepo.save(newDcUser);
	}

	/**
	 * Filters the user name from the raw command message.
	 *
	 * @param rawMessage The raw command message without Discord displaying content differently.
	 * @return {@code Optional<String>} which is empty if there is no user name given in quotation marks. If there is
	 * one given the {@code Optional<String>} contains the user name.
	 */
	private Optional<String> getUserName(final String rawMessage) {
		if (!rawMessage.matches(".*\"(.|\\n)*\".*")) {
			return Optional.empty();
		}

		return Optional.of(rawMessage.substring(rawMessage.indexOf('"') + 1, rawMessage.lastIndexOf('"')));
	}

	/**
	 * Assigns the forum roles to the added user in the guild where the command got triggered if the member is
	 * in the guild.
	 *
	 * @param channel The channel the command got triggered in.
	 * @param newUser The user that got added.
	 */
	private void assignUserRoles(final TextChannel channel, final ForumUser newUser) {
		final Guild guild = channel.getGuild();
		guild.retrieveMemberById(newUser.getLinkedDiscordUser().getDiscordId()).queue(
				member -> assignMemberRoles(channel, newUser, member)
		);
	}

	/**
	 * Assigns the forum roles to the member who got added to the database.
	 * Every other guild the user and the bot are in will only update via {@link RoleUpdater}.
	 *
	 * @param channel The channel the command got triggered in.
	 * @param newUser The user that got added.
	 * @param member  The Discord member object that matches the linked {@link DiscordUser}
	 *                of {@param newUser}.
	 */
	private void assignMemberRoles(final TextChannel channel, final ForumUser newUser, final Member member) {
		final List<ForumRole> forumRoles;
		try {
			forumRoles = forumRoleApiRequest.getRolesOfForumUser(newUser);
		} catch (IOException e) {
			sendErrorMessage(channel, "Could not get roles of user!");
			LogUtil.logError("Could not get roles of " + newUser.toString(), e);
			return;
		}

		if (RoleUtil.hasBannedRole(envSettings, forumRoles)) {
			member.ban(0, "User (" + newUser.getForumId() + ") has the banned role on the forum. Might be a temporary ban.").queue();
			sendErrorMessage(channel, "Member has the banned role on the forum and thus has been banned.");
			return;
		}

		RoleUtil.updateRoles(member, forumRoles, forumRoleRepo.findAll());
		answer(channel, "Added new user to the database!");
	}

	/**
	 * Logs the add action.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param user  The added user.
	 */
	private void doUserAddLog(final GuildMessageReceivedEvent event, final ForumUser user) {
		final String authorId = event.getAuthor().getId();
		final String author = event.getAuthor().getAsTag();
		final String message = author + " (" + authorId + ")" + " added a user to the database: " + user.toString() + ".";
		LogUtil.logInfo(message);
	}
}
