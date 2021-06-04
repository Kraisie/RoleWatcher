package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.bot.service.ForumRoleApiRequest;
import com.motorbesitzen.rolewatcher.bot.service.RoleUpdater;
import com.motorbesitzen.rolewatcher.data.dao.*;
import com.motorbesitzen.rolewatcher.data.repo.*;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.RoleUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Command to let users verify the link between their forum and Discord accounts.
 */
@Service("verify")
public class Verify extends CommandImpl {

	private final EnvSettings envSettings;
	private final DiscordGuildRepo guildRepo;
	private final DiscordUserRepo dcUserRepo;
	private final ForumUserRepo forumUserRepo;
	private final ForumRoleApiRequest forumRoleApiRequest;
	private final ForumRoleRepo forumRoleRepo;
	private final LinkingInformationRepo infoRepo;

	@Autowired
	private Verify(final EnvSettings envSettings, final DiscordGuildRepo guildRepo, final DiscordUserRepo dcUserRepo,
				   final ForumUserRepo forumUserRepo, final ForumRoleApiRequest forumRoleApiRequest,
				   final ForumRoleRepo forumRoleRepo, final LinkingInformationRepo infoRepo) {
		this.envSettings = envSettings;
		this.guildRepo = guildRepo;
		this.dcUserRepo = dcUserRepo;
		this.forumUserRepo = forumUserRepo;
		this.forumRoleApiRequest = forumRoleApiRequest;
		this.forumRoleRepo = forumRoleRepo;
		this.infoRepo = infoRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "verify";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsAuthorization() {
		return false;
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
		return false;
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
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		if (dcGuildOpt.isPresent()) {
			final long channelId = event.getChannel().getIdLong();
			final long verificationChannelId = dcGuildOpt.get().getVerificationChannelId();
			if (channelId != verificationChannelId) {
				return;
			}
		}

		final Message message = event.getMessage();
		final User author = message.getAuthor();
		final String content = message.getContentRaw();
		final String[] tokens = content.split(" ");
		if (tokens.length != 2) {
			final String prefix = envSettings.getCommandPrefix();
			sendTemporaryErrorMessage(
					event.getChannel(),
					author.getAsMention() + " please use the correct syntax: `" + prefix + "verify <code>`\n" +
							"Insert the `code` you got on the forum for `<code>` and remove the `<` and `>`!",
					30
			);
			message.delete().queue();
			return;
		}

		final String verificationCode = tokens[tokens.length - 1];
		if (verificationCode.length() < 1 || verificationCode.length() > 20) {
			sendTemporaryErrorMessage(
					event.getChannel(),
					author.getAsMention() + " please check the validity of your code and try again.",
					30
			);
			message.delete().queue();
			return;
		}

		final Optional<LinkingInformation> savedInfo = infoRepo.findByVerificationCode(verificationCode);
		savedInfo.ifPresentOrElse(
				linkingInformation -> linkUser(event.getChannel(), linkingInformation, author.getIdLong()),
				() -> sendTemporaryErrorMessage(
						event.getChannel(),
						author.getAsMention() + " please check the validity of your code and try again.",
						30
				)
		);

		message.delete().queue();
	}

	private void linkUser(final TextChannel channel, final LinkingInformation linkingInformation, final long discordId) {
		final long forumId = linkingInformation.getUid();
		final Optional<ForumUser> forumUserByUidOpt = forumUserRepo.findById(forumId);
		if (forumUserByUidOpt.isPresent()) {
			sendTemporaryErrorMessage(
					channel,
					"Your forum account is already linked! Contact a staff if you want to get unlinked.",
					30
			);
			return;
		}

		final Optional<ForumUser> forumUserByDcIdOpt = forumUserRepo.findByLinkedDiscordUser_DiscordId(discordId);
		if (forumUserByDcIdOpt.isPresent()) {
			sendTemporaryErrorMessage(
					channel,
					"Your Discord account is already linked! Contact a staff if you want to get unlinked.",
					30
			);
			return;
		}

		final String username = linkingInformation.getForumUsername();
		final ForumUser newForumUser = ForumUser.create(forumId, username);
		final Optional<DiscordUser> dcUserOpt = dcUserRepo.findById(discordId);
		dcUserOpt.ifPresentOrElse(dcUser -> addForumUserLink(dcUser, newForumUser), () -> createDiscordUserLink(discordId, newForumUser));
		infoRepo.delete(linkingInformation);
		assignUserRoles(channel.getGuild(), newForumUser);
		LogUtil.logDebug("Linked user: " + newForumUser);
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
		dcUserRepo.save(dcUser);
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
		dcUserRepo.save(newDcUser);
	}

	/**
	 * Assigns the forum roles to the added user in the guild where the command got triggered if the member is
	 * in the guild.
	 *
	 * @param guild   The guild the command got triggered in.
	 * @param newUser The user that got added.
	 */
	private void assignUserRoles(final Guild guild, final ForumUser newUser) {
		guild.retrieveMemberById(newUser.getLinkedDiscordUser().getDiscordId()).queue(
				member -> assignMemberRoles(newUser, member)
		);
	}

	/**
	 * Assigns the forum roles to the member who got added to the database.
	 * Every other guild the user and the bot are in will only update via {@link RoleUpdater}.
	 *
	 * @param newUser The user that got added.
	 * @param member  The Discord member object that matches the linked {@link DiscordUser}
	 *                of {@param newUser}.
	 */
	private void assignMemberRoles(final ForumUser newUser, final Member member) {
		final List<ForumRole> forumRoles;
		try {
			forumRoles = forumRoleApiRequest.getRolesOfForumUser(newUser);
		} catch (IOException e) {
			LogUtil.logError("[VERIFY] Could not get roles of " + newUser.toString(), e);
			return;
		}

		if (RoleUtil.hasBannedRole(envSettings, forumRoles)) {
			member.ban(0, "User (" + newUser.getForumId() + ") has the banned role on the forum. Might be a temporary ban.").queue();
			return;
		}

		RoleUtil.updateRoles(member, forumRoles, forumRoleRepo.findAll());
	}
}
