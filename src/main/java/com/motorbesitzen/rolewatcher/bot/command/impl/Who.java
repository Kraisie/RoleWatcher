package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Optional;

/**
 * Sends information about a forum/Discord user if available.
 */
public class Who extends CommandImpl {

	private final DiscordUserRepo dcUserRepo;
	private final ForumUserRepo fUserRepo;

	public Who(final DiscordUserRepo dcUserRepo, final ForumUserRepo fUserRepo) {
		this.dcUserRepo = dcUserRepo;
		this.fUserRepo = fUserRepo;
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
	 * @param channel   The channel where the command got triggered and the bot will reply in.
	 * @param fUserOpt  An {@code Optional} that may contain a forum user if found in the database for an ID.
	 * @param dcUserOpt An {@code Optional} that may contain a Discord user if found in the database for an ID.
	 */
	private void sendInfoMessage(final TextChannel channel, final Optional<ForumUser> fUserOpt, final Optional<DiscordUser> dcUserOpt) {
		final MessageEmbed embed = buildEmbedMessage(channel, fUserOpt, dcUserOpt);
		answer(channel, embed);
	}

	/**
	 * Builds an embedded message with info about the Discord and/or forum user.
	 *
	 * @param channel   The channel where the command got triggered.
	 * @param fUserOpt  An {@code Optional} that may contain a forum user if found in the database for an ID.
	 * @param dcUserOpt An {@code Optional} that may contain a Discord user if found in the database for an ID.
	 * @return The {@code MessageEmbed} to send in a channel.
	 */
	private MessageEmbed buildEmbedMessage(final TextChannel channel, final Optional<ForumUser> fUserOpt, final Optional<DiscordUser> dcUserOpt) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("User information: ");
		eb.setColor(getEmbedColor());
		dcUserOpt.ifPresentOrElse(dcUser -> setDiscordInfo(channel.getGuild(), eb, dcUser), () -> eb.addField("Discord user:", "UNKNOWN", true));
		eb.addBlankField(false);
		fUserOpt.ifPresentOrElse(fUser -> setForumInfo(eb, fUser), () -> eb.addField("Forum user:", "UNKNOWN (not linked)", true));
		return eb.build();
	}

	/**
	 * Sets the discord info for a user to the info embed message.
	 *
	 * @param guild  The guild where the command got requested
	 * @param eb     The EmbedBuilder for the message.
	 * @param dcUser The Discord user to show info about.
	 */
	private void setDiscordInfo(final Guild guild, final EmbedBuilder eb, final DiscordUser dcUser) {
		final Member member = guild.retrieveMemberById(dcUser.getDiscordId()).complete();
		if (member != null) {
			User user = member.getUser();
			eb.setAuthor("Found user!", null, user.getAvatarUrl());
			eb.addField("Discord user:", member.getAsMention(), true);
		} else {
			// if no default avatar url is provided use none by giving the author field a null URL thus not showing any avatar
			String iconUrl = EnvironmentUtil.getEnvironmentVariable("DEFAULT_AVATAR_URL");
			eb.setAuthor("Found user!", null, iconUrl);
			eb.addField("Discord user:", "<@" + dcUser.getDiscordId() + ">", true);
		}
		eb.addField("Whitelisted?", dcUser.isWhitelisted() ? "Yes" : "No", true);
	}

	/**
	 * Sets the forum info for a user to the info embed message.
	 *
	 * @param eb    The EmbedBuilder for the message.
	 * @param fUser The forum user to show info about.
	 */
	private void setForumInfo(final EmbedBuilder eb, final ForumUser fUser) {
		String link =
				EnvironmentUtil.getEnvironmentVariable("FORUM_MEMBER_PROFILE_URL") == null ?
						"Forum link not set!" :
						EnvironmentUtil.getEnvironmentVariable("FORUM_MEMBER_PROFILE_URL") + fUser.getForumId();
		eb.addField("Forum user:", fUser.getForumUsername() + " (UID: " + fUser.getForumId() + ")", false);
		eb.addField("Link: ", link, false);
	}
}
