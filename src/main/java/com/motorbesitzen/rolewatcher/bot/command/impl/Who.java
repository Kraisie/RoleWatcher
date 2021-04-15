package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sends information about a forum/Discord user if available.
 */
@Service("who")
class Who extends CommandImpl {

	private final EnvSettings envSettings;
	private final DiscordUserRepo dcUserRepo;
	private final ForumUserRepo fUserRepo;

	@Autowired
	private Who(final EnvSettings envSettings, final DiscordUserRepo dcUserRepo, final ForumUserRepo fUserRepo) {
		this.envSettings = envSettings;
		this.dcUserRepo = dcUserRepo;
		this.fUserRepo = fUserRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "who";
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
		return "Shows information about a specific user. Can be used with a Discord tag, " +
				"a Discord ID or a forum ID.";
	}

	/**
	 * Sends a message with information about a forum or discord user by ID.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final Message message = event.getMessage();

		final long id = DiscordMessageUtil.getMentionedMemberId(message);
		if (id == -1) {
			sendErrorMessage(channel, "Please mention a user or provide an ID to receive the user information.");
			return;
		}

		final Optional<ForumUser> fUserOpt = fUserRepo.findByForumIdOrLinkedDiscordUser_DiscordId(id, id);
		final Optional<DiscordUser> dcUserOpt = fUserOpt.map(forumUser ->
				dcUserRepo.findById(forumUser.getLinkedDiscordUser().getDiscordId())).orElseGet(() ->
				dcUserRepo.findById(id));

		if (fUserOpt.isEmpty() && dcUserOpt.isEmpty()) {
			sendErrorMessage(channel, "No user found with that ID!");
			return;
		}

		sendInfoMessage(channel, fUserOpt, dcUserOpt);
	}

	/**
	 * Sends an embedded message with info about the Discord and/or forum user.
	 *
	 * @param channel   The channel where the command got triggered.
	 * @param fUserOpt  An {@code Optional} that may contain a forum user if found in the database for an ID.
	 * @param dcUserOpt An {@code Optional} that may contain a Discord user if found in the database for an ID.
	 */
	private void sendInfoMessage(final TextChannel channel, final Optional<ForumUser> fUserOpt, final Optional<DiscordUser> dcUserOpt) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("User information: ");
		eb.setColor(envSettings.getEmbedColor());
		dcUserOpt.ifPresentOrElse(
				dcUser -> setUserInfo(channel, eb, dcUser, fUserOpt),
				() -> setUnknownUserInfo(eb)
		);
	}

	/**
	 * Adds the info for a user to the info embed message.
	 *
	 * @param channel  The channel where the command got requested in.
	 * @param eb       The EmbedBuilder for the message.
	 * @param dcUser   The Discord user to show info about.
	 * @param fUserOpt An {@code Optional} that may contain a forum user if found in the database for an ID.
	 */
	private void setUserInfo(final TextChannel channel, final EmbedBuilder eb, final DiscordUser dcUser, final Optional<ForumUser> fUserOpt) {
		channel.getGuild().retrieveMemberById(dcUser.getDiscordId()).queue(
				member -> {
					setMemberInfo(eb, dcUser, member);
					eb.addBlankField(false);
					setForumUserInfo(eb, fUserOpt);
					answer(channel, eb.build());
				},
				throwable -> {
					setDefaultInfo(eb, dcUser);
					eb.addBlankField(false);
					setForumUserInfo(eb, fUserOpt);
					answer(channel, eb.build());
				}
		);
	}

	/**
	 * Adds Discord member information to the embed.
	 *
	 * @param eb     The EmbedBuilder for the message.
	 * @param member The Discord member for the given ID.
	 * @param dcUser The Discord user to show info about.
	 */
	private void setMemberInfo(final EmbedBuilder eb, final DiscordUser dcUser, final Member member) {
		final User user = member.getUser();
		eb.setAuthor("Found user!", null, user.getAvatarUrl());
		eb.addField("Discord user:", member.getAsMention(), true);
		setWhitelistInfo(eb, dcUser);
	}

	/**
	 * Adds default information to the embed if the user is not in the guild.
	 *
	 * @param eb     The EmbedBuilder for the message.
	 * @param dcUser The Discord user to show info about.
	 */
	private void setDefaultInfo(final EmbedBuilder eb, final DiscordUser dcUser) {
		final String iconUrl = envSettings.getDefaultAvatarUrl();
		eb.setAuthor("Found user!", null, iconUrl);
		eb.addField("Discord user:", "<@" + dcUser.getDiscordId() + ">", true);
		setWhitelistInfo(eb, dcUser);
	}

	/**
	 * Adds information to the embed on whether or not the user is whitelisted.
	 *
	 * @param eb     The EmbedBuilder for the message.
	 * @param dcUser The Discord user to show info about.
	 */
	private void setWhitelistInfo(final EmbedBuilder eb, final DiscordUser dcUser) {
		eb.addField("Whitelisted?", dcUser.isWhitelisted() ? "Yes" : "No", true);
	}

	/**
	 * Adds information that the Discord user is unknown.
	 *
	 * @param eb The EmbedBuilder for the message.
	 */
	private void setUnknownUserInfo(final EmbedBuilder eb) {
		eb.addField("Discord user:", "UNKNOWN", true);
	}

	/**
	 * Adds information about the linked forum user for a Discord user if given. Otherwise adds
	 * the info that the forum user is unknown.
	 *
	 * @param eb       The EmbedBuilder for the message.
	 * @param fUserOpt An {@code Optional} that may contain a forum user if found in the database for an ID.
	 */
	private void setForumUserInfo(final EmbedBuilder eb, final Optional<ForumUser> fUserOpt) {
		fUserOpt.ifPresentOrElse(
				fUser -> setForumInfo(eb, fUser),
				() -> eb.addField("Forum user:", "UNKNOWN (not linked)", true)
		);
	}

	/**
	 * Sets the forum info for a user to the info embed message.
	 *
	 * @param eb    The EmbedBuilder for the message.
	 * @param fUser The forum user to show info about.
	 */
	private void setForumInfo(final EmbedBuilder eb, final ForumUser fUser) {
		final String link =
				envSettings.getForumMemberProfileUrl() == null ?
						"Forum link not set!" :
						envSettings.getForumMemberProfileUrl() + fUser.getForumId();
		eb.addField("Forum user:", fUser.getForumUsername() + " (UID: " + fUser.getForumId() + ")", false);
		eb.addField("Link: ", link, false);
	}
}
