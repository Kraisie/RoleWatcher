package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.data.dao.AuthedChannel;
import com.motorbesitzen.rolewatcher.data.dao.AuthedRole;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.AuthedChannelRepo;
import com.motorbesitzen.rolewatcher.data.repo.AuthedRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Sends basic data of the guild and its settings.
 */
@Service("info")
class Info extends CommandImpl {

	private final EnvSettings envSettings;
	private final DiscordGuildRepo guildRepo;
	private final AuthedChannelRepo channelRepo;
	private final AuthedRoleRepo roleRepo;

	@Autowired
	private Info(final EnvSettings envSettings, final DiscordGuildRepo guildRepo, final AuthedChannelRepo channelRepo,
				 final AuthedRoleRepo roleRepo) {
		this.envSettings = envSettings;
		this.guildRepo = guildRepo;
		this.channelRepo = channelRepo;
		this.roleRepo = roleRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "info";
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
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Lists all authorized channels and roles and shows if the guild has read or write permissions to the database.";
	}

	/**
	 * Sends an information message about the permissions the guild has, its authorized roles and  its authorized channels.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> discordGuildOpt = guildRepo.findById(guildId);
		discordGuildOpt.ifPresent(discordGuild -> sendInfo(event, discordGuild));
	}

	/**
	 * Gathers all needed data, creates the message and sends it.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 * @param guild The Discord guild object which contains the permissions.
	 */
	private void sendInfo(final GuildMessageReceivedEvent event, final DiscordGuild guild) {
		final Set<AuthedChannel> authedChannels = channelRepo.findAllByGuild_GuildId(guild.getGuildId());
		final Set<AuthedRole> authedRoles = roleRepo.findAllByGuild_GuildId(guild.getGuildId());

		final MessageEmbed embedInfo = buildEmbed(event, guild, authedChannels, authedRoles);
		answer(event.getChannel(), embedInfo);
	}

	/**
	 * Builds the embedded message.
	 *
	 * @param event          The event provided by JDA that a guild message got received.
	 * @param guild          The Discord guild object which contains the permissions.
	 * @param authedChannels A list of all authorized Discord channels.
	 * @param authedRoles    A list of all authorized Discord roles.
	 * @return The data as {@code MessageEmbed}.
	 */
	private MessageEmbed buildEmbed(final GuildMessageReceivedEvent event, final DiscordGuild guild, final Set<AuthedChannel> authedChannels, final Set<AuthedRole> authedRoles) {
		final String channelContent = buildAuthedChannelList(authedChannels);
		final String roleContent = buildAuthedRoleList(authedRoles);

		return new EmbedBuilder()
				.setTitle("Info for \"" + event.getGuild().getName() + "\":")
				.setColor(envSettings.getEmbedColor())
				.addField("Read Permission: ", (guild.hasReadPerm() ? "Yes" : "No"), true)
				.addField("Write Permission: ", (guild.hasWritePerm() ? "Yes" : "No"), true)
				.addBlankField(false)
				.addField("Authorized channels: ", channelContent, true)
				.addField("Authorized roles: ", roleContent, true)
				.addBlankField(false)
				.addField("Autokick enabled?", guild.canAutokick() ? "Yes" : "No", true)
				.addField("Autokick delay:", guild.getAutokickHourDelay() + "h", true)
				.addBlankField(false)
				.setFooter(
						"If no channels are authorized commands can be used everywhere. " +
								"If no roles are authorized only the owner of the guild can use commands."
				).build();
	}

	/**
	 * Builds the authorized channels list as a list of mentions.
	 *
	 * @param authedChannels A list of all authorized Discord channels.
	 * @return String that mentions all authorized channels in Discord mention style.
	 */
	private String buildAuthedChannelList(final Set<AuthedChannel> authedChannels) {
		if (authedChannels.size() == 0) {
			return "No channels authorized.";
		}

		final StringBuilder sb = new StringBuilder();
		for (AuthedChannel channel : authedChannels) {
			sb.append("<#").append(channel.getChannelId()).append(">\n");
		}

		return sb.toString();
	}

	/**
	 * Builds the authorized roles list as a list of mentions.
	 *
	 * @param authedRoles A list of all authorized Discord roles.
	 * @return String that mentions all authorized roles in Discord mention style.
	 */
	private String buildAuthedRoleList(final Set<AuthedRole> authedRoles) {
		if (authedRoles.size() == 0) {
			return "No roles authorized.";
		}

		final StringBuilder sb = new StringBuilder();
		for (AuthedRole role : authedRoles) {
			sb.append("<@&").append(role.getRoleId()).append(">\n");
		}

		return sb.toString();
	}
}
