package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.AuthedChannel;
import com.motorbesitzen.rolewatcher.data.dao.AuthedRole;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.AuthedChannelRepo;
import com.motorbesitzen.rolewatcher.data.repo.AuthedRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Used to authorize Discord text channels and Discord roles to use bot commands.
 */
@Service("authorize")
class Authorize extends CommandImpl {

	private final AuthedChannelRepo channelRepo;
	private final AuthedRoleRepo roleRepo;
	private final DiscordGuildRepo discordGuildRepo;

	@Autowired
	private Authorize(final AuthedChannelRepo channelRepo, final AuthedRoleRepo roleRepo, final DiscordGuildRepo discordGuildRepo) {
		this.channelRepo = channelRepo;
		this.roleRepo = roleRepo;
		this.discordGuildRepo = discordGuildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "authorize";
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
		return getName() + " (#channel|@role)+";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Authorizes a role for using commands or a channel to use commands in. Can be used with " +
				"multiple discord tags for channels and/or roles.";
	}

	/**
	 * Authorizes mentioned roles and channels in a message. If no roles or channels are mentioned
	 * it sends an error message in the chat where the command got used telling the user to mention
	 * a role or a channel. If all of the mentioned channels/roles are already authorized it still replies with
	 * the message that the channels/roles got authorized!
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final TextChannel channel = event.getChannel();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		final List<Role> mentionedRoles = message.getMentionedRoles();
		if (mentionedChannels.size() == 0 && mentionedRoles.size() == 0) {
			sendErrorMessage(event.getChannel(), "Please provide one or more channels and/or roles to authorize for command usage.");
			return;
		}

		long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = discordGuildRepo.findById(guildId);
		dcGuildOpt.ifPresent(guild -> authorize(channel, guild, mentionedChannels, mentionedRoles));
	}

	/**
	 * Authorizes roles and channels and sends success message.
	 *
	 * @param channel           The channel in which the command got used.
	 * @param guild             The database entry for the guild where the command got used.
	 * @param mentionedChannels The list of channels to authorize for command usage.
	 * @param mentionedRoles    The list of roles to authorize for command usage.
	 */
	private void authorize(final TextChannel channel, final DiscordGuild guild, final List<TextChannel> mentionedChannels, final List<Role> mentionedRoles) {
		if (mentionedChannels.size() != 0) {
			authorizeChannels(guild, mentionedChannels);
			answer(channel, "Authorized the mentioned channel(s).");
		}

		if (mentionedRoles.size() != 0) {
			authorizeRoles(guild, mentionedRoles);
			answer(channel, "Authorized the mentioned role(s).");
		}
	}

	/**
	 * Authorizes mentioned channels if they are not yet authorized.
	 *
	 * @param guild    The database entry for the guild where the command got used.
	 * @param channels The mentioned channels in the command message.
	 */
	private void authorizeChannels(final DiscordGuild guild, final List<TextChannel> channels) {
		for (TextChannel channel : channels) {
			if (channelRepo.existsById(channel.getIdLong())) {
				continue;
			}

			final AuthedChannel authedChannel = new AuthedChannel(channel.getIdLong(), guild);
			channelRepo.save(authedChannel);
		}
	}

	/**
	 * Authorizes mentioned roles if they are not yet authorized.
	 *
	 * @param guild The database entry for the guild where the command got used.
	 * @param roles The mentioned roles in the command message.
	 */
	private void authorizeRoles(final DiscordGuild guild, final List<Role> roles) {
		for (Role role : roles) {
			if (roleRepo.existsById(role.getIdLong())) {
				continue;
			}

			final AuthedRole authedRole = new AuthedRole(role.getIdLong(), guild);
			roleRepo.save(authedRole);
		}
	}
}
