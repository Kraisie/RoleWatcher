package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.bot.command.CommandInfo;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Sends a help message with information about all available commands to the channel where the help was requested.
 */
@Service("help")
public class Help extends CommandImpl {

	private final DiscordGuildRepo discordGuildRepo;

	@Autowired
	public Help(final DiscordGuildRepo discordGuildRepo) {
		this.discordGuildRepo = discordGuildRepo;
	}

	/**
	 * Sends a help message for the commands the guild can use.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final TextChannel channel = event.getChannel();
		final List<CommandInfo> commands = CommandInfo.getAllCommandInfos();
		final Optional<DiscordGuild> guildOpt = discordGuildRepo.findById(guildId);
		guildOpt.ifPresent(guild -> sendHelpMessage(channel, guild, commands));
	}

	/**
	 * Sends the help message in the channel where the help got requested.
	 *
	 * @param channel  The channel in which the command got used.
	 * @param guild    The database entry for the guild where the command got used.
	 * @param commands The list of commands the bot can execute.
	 */
	private void sendHelpMessage(final TextChannel channel, final DiscordGuild guild, final List<CommandInfo> commands) {
		final EmbedBuilder eb = buildHelpMessage(guild, commands);
		final MessageEmbed embed = eb.build();
		answer(channel, embed);
	}

	/**
	 * Builds the help message.
	 *
	 * @param guild    The database entry for the guild where the command got used.
	 * @param commands The list of commands the bot can execute.
	 * @return An {@code EmbedBuilder} that contains the help information.
	 */
	private EmbedBuilder buildHelpMessage(final DiscordGuild guild, final List<CommandInfo> commands) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(getEmbedColor());
		eb.setTitle("Commands and their variations")
				.setDescription("A list of all commands you can use and what they do. " +
						"Note that \"(a|b|c)\" means that a, b or c can be chosen.");
		eb.setFooter("If you are missing some functionality contact the owner of the bot to update your permissions.");

		addHelpEntries(guild, commands, eb);
		return eb;
	}

	/**
	 * Adds an entry for each command. Does not add commands that the guild has no permission to use.
	 * Ignores commands that only the owner of the bot can use completely.
	 *
	 * @param guild    The database entry for the guild where the command got used.
	 * @param commands The list of commands the bot can execute.
	 * @param eb       The {@code EmbedBuilder} to which each commands help information gets.
	 */
	private void addHelpEntries(final DiscordGuild guild, final List<CommandInfo> commands, final EmbedBuilder eb) {
		final String prefix = EnvironmentUtil.getEnvironmentVariableOrDefault("CMD_PREFIX", "");
		for (CommandInfo command : commands) {
			if (!canUseCommand(guild, command)) {
				continue;
			}

			final String title = prefix + command.getUsage();
			eb.addField(title, command.getDescription(), false);
		}
	}

	/**
	 * Checks if the given guild is allowed to use a specific command.
	 *
	 * @param guild   The database entry for the guild where the command got used.
	 * @param command The command in question.
	 * @return {@code true} if the guild has the needed permissions for the command, {@code false} if not.
	 */
	private boolean canUseCommand(final DiscordGuild guild, final CommandInfo command) {
		if (command.needsOwnerPerms()) {
			return false;
		}

		if (command.needsReadPerms() && !guild.hasReadPerm()) {
			return false;
		}

		return !command.needsWritePerms() || guild.hasWritePerm();
	}
}
