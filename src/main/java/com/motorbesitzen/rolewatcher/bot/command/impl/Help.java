package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.Command;
import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sends a help message with information about all available commands to the channel where the help was requested.
 */
@Service("help")
class Help extends CommandImpl {

	private final Map<String, Command> commandMap;
	private final DiscordGuildRepo discordGuildRepo;

	private static final int FIELDS_PER_EMBED = 25;

	@Autowired
	private Help(final Map<String, Command> commandMap, final DiscordGuildRepo discordGuildRepo) {
		this.commandMap = commandMap;
		this.discordGuildRepo = discordGuildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "help";
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
		return "Shows a list of commands that can be used.";
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
		final Optional<DiscordGuild> guildOpt = discordGuildRepo.findById(guildId);
		guildOpt.ifPresent(guild -> sendHelpMessage(channel, guild));
	}

	/**
	 * Sends the help message in the channel where the help got requested.
	 *
	 * @param channel The channel in which the command got used.
	 * @param guild   The database entry for the guild where the command got used.
	 */
	private void sendHelpMessage(final TextChannel channel, final DiscordGuild guild) {
		final List<Command> commands = new ArrayList<>(commandMap.values());
		if (commands.size() == 0) {
			sendErrorMessage(channel, "No commands found!");
			return;
		}

		final int pages = (commands.size() / FIELDS_PER_EMBED) + 1;
		EmbedBuilder eb = buildEmbedPage(1, pages);
		for (int i = 0; i < commands.size(); i++) {
			if (i > 0 && i % 25 == 0) {
				answer(channel, eb.build());
				eb = buildEmbedPage((i / FIELDS_PER_EMBED) + 1, pages);
			}

			final Command command = commands.get(i);
			addHelpEntry(guild, eb, command);
		}

		answer(channel, eb.build());
	}

	/**
	 * Creates a numerated page for help entries. Can have up to 25 command fields.
	 *
	 * @param page       The current page number.
	 * @param totalPages The total pages needed to display all commands
	 * @return An {@code EmbedBuilder} with page identification if needed.
	 */
	private EmbedBuilder buildEmbedPage(final int page, final int totalPages) {
		return new EmbedBuilder().setColor(
				getEmbedColor()
		).setTitle(
				page == 1 && totalPages == 1 ?
						"Commands and their variations" :
						"Commands and their variations [" + page + "/" + totalPages + "]"
		).setDescription(
				"A list of all commands you can use and what they do. " +
						"Note that \"(a|b|c)\" means that a, b or c can be chosen."
		).setFooter(
				"If you are missing some functionality contact the owner of the bot to update your permissions."
		);
	}

	/**
	 * Adds an entry for a command. Does not add commands that the guild has no permission to use.
	 * Ignores commands that only the owner of the bot can use.
	 *
	 * @param guild   The database entry for the guild where the help command got used.
	 * @param eb      The {@code EmbedBuilder} to which each commands help information gets.
	 * @param command The command to add to the help page.
	 */
	private void addHelpEntry(final DiscordGuild guild, final EmbedBuilder eb, final Command command) {
		final String prefix = EnvironmentUtil.getEnvironmentVariableOrDefault("CMD_PREFIX", "");
		if (!canUseCommand(guild, command)) {
			return;
		}

		final String title = prefix + command.getUsage();
		eb.addField(title, command.getDescription(), false);
	}

	/**
	 * Checks if the given guild is allowed to use a specific command.
	 *
	 * @param guild   The database entry for the guild where the command got used.
	 * @param command The command in question.
	 * @return {@code true} if the guild has the needed permissions for the command, {@code false} if not.
	 */
	private boolean canUseCommand(final DiscordGuild guild, final Command command) {
		if (command.needsOwnerPerms()) {
			return false;
		}

		if (command.needsReadPerms() && !guild.hasReadPerm()) {
			return false;
		}

		return !command.needsWritePerms() || guild.hasWritePerm();
	}
}
