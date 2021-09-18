package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordBanRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Sends information about a forum/Discord user if available.
 */
@Service("who")
class Who extends CommandImpl {

	private final EnvSettings envSettings;
	private final DiscordUserRepo dcUserRepo;
	private final ForumUserRepo fUserRepo;
	private final DiscordBanRepo banRepo;

	// public to be able to use @Transactional on execute()
	@Autowired
	public Who(final EnvSettings envSettings, final DiscordUserRepo dcUserRepo, final ForumUserRepo fUserRepo,
			   final DiscordBanRepo banRepo) {
		this.envSettings = envSettings;
		this.dcUserRepo = dcUserRepo;
		this.fUserRepo = fUserRepo;
		this.banRepo = banRepo;
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
	@Transactional
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
		final long guildId = channel.getGuild().getIdLong();
		setDiscordUserInfo(eb, dcUser);
		setBanInfo(eb, dcUser, guildId);
		eb.addBlankField(false);
		setForumUserInfo(eb, fUserOpt);
		answer(channel, eb.build());
	}

	/**
	 * Adds Discord member information to the embed.
	 *
	 * @param eb     The EmbedBuilder for the message.
	 * @param dcUser The Discord user to show info about.
	 */
	private void setDiscordUserInfo(final EmbedBuilder eb, final DiscordUser dcUser) {
		eb.addField("Discord user:", "<@" + dcUser.getDiscordId() + ">", true);
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
	 * Adds the ban reason to the info of the user if there is one for this guild.
	 *
	 * @param eb      The EmbedBuilder for the message.
	 * @param dcUser  The Discord user to show info about.
	 * @param guildId The ID of the guild in which the info got requested.
	 */
	private void setBanInfo(final EmbedBuilder eb, final DiscordUser dcUser, final long guildId) {
		final Optional<DiscordBan> banOpt = dcUser.getBanForGuild(guildId);
		banOpt.ifPresent(
				ban -> eb.addField("Banned for:", ban.getReason(), false)
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
