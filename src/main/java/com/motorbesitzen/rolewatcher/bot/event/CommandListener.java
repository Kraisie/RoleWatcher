package com.motorbesitzen.rolewatcher.bot.event;

import com.motorbesitzen.rolewatcher.bot.command.Command;
import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.data.dao.AuthedChannel;
import com.motorbesitzen.rolewatcher.data.dao.AuthedRole;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.AuthedChannelRepo;
import com.motorbesitzen.rolewatcher.data.repo.AuthedRoleRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Checks all incoming messages for commands and the needed permissions to use it. If all data is correct it
 * executes the command. Commands are limited to messages inside of guilds so this listener does not check for
 * messages sent in a private channel (like direct messages).
 */
@Service
public class CommandListener extends ListenerAdapter {

	/**
	 * Contains all Command subclasses (and CommandImpl itself) which are registered as a Bean
	 */
	private final Map<String, Command> commandMap;
	private final EnvSettings envSettings;
	private final DiscordGuildRepo guildRepo;
	private final AuthedChannelRepo channelRepo;
	private final AuthedRoleRepo roleRepo;

	/**
	 * Private constructor to be used by Spring autowiring.
	 *
	 * @param commandMap A {@code Map} of Beans that implement the {@link Command}
	 *                   interface. The map contains the name of the Bean  as {@code String}
	 *                   (key) and the implementation (value).
	 */
	@Autowired
	private CommandListener(final Map<String, Command> commandMap, final EnvSettings envSettings,
							final DiscordGuildRepo guildRepo, final AuthedChannelRepo channelRepo,
							final AuthedRoleRepo roleRepo) {
		this.commandMap = commandMap;
		this.envSettings = envSettings;
		this.guildRepo = guildRepo;
		this.channelRepo = channelRepo;
		this.roleRepo = roleRepo;
	}

	/**
	 * Gets triggered by a JDA <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">GuildMessageReceivedEvent</a>
	 * which gets fired each time the bot receives a message in a <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 * of a <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>.
	 * Performs all needed steps to verify if a message is a valid command by an authorized
	 * <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Member.html">Member</a>
	 * in an authorized <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 * in a <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a> that
	 * has the needed permissions. Calls the commands method to execute the command on success.
	 *
	 * @param event The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">GuildMessageReceivedEvent</a>
	 *              provided by JDA.
	 */
	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		// check if valid message
		final long guildId = event.getGuild().getIdLong();
		final DiscordGuild dcGuild = getDiscordGuild(guildId);
		final Message message = event.getMessage();
		if (!isValidMessage(message)) {
			deleteInVerify(dcGuild, message);
			return;
		}

		// check if valid command prefix
		final TextChannel channel = event.getChannel();
		final String cmdPrefix = envSettings.getCommandPrefix();
		final String messageContent = message.getContentRaw();
		if (!isValidCommandPrefix(cmdPrefix, messageContent)) {
			deleteInVerify(dcGuild, message);
			return;
		}

		// identify command
		final String commandName = identifyCommandName(cmdPrefix, messageContent);
		final Command command = commandMap.get(commandName);
		if (command == null) {
			deleteInVerify(dcGuild, message);
			return;
		}

		final Member author = message.getMember();
		if (author == null) {
			return;
		}

		// check if command can only be used by owner of the bot and if the caller is not the owner of the bot
		if (command.needsOwnerPerms() && !isCallerBotOwner(author)) {
			deleteInVerify(dcGuild, message);
			return;
		}

		// check if role/channel is authorized
		if (command.needsAuthorization()) {
			if (!isAuthorizedUsage(author, channel)) {
				deleteInVerify(dcGuild, message);
				return;
			}
		}

		// check if channel is valid for command usage
		if (!isValidChannel(channel)) {
			deleteInVerify(dcGuild, message);
			return;
		}

		// check if guild is unauthorized
		if (!isAuthorizedGuild(dcGuild, command)) {
			deleteInVerify(dcGuild, message);
			return;
		}

		executeCommand(event, command);
	}

	/**
	 * Checks if the bot can send messages in the channel where it received the message.
	 *
	 * @param channel The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                in which the message got sent.
	 * @return {@code true} if the channel is valid as the bot can answer to a command, {@code false} if the bot can not
	 * send messages in the channel
	 */
	private boolean isValidChannel(final TextChannel channel) {
		return channel.canTalk();
	}

	/**
	 * Checks if the message is invalid e.g. due to the author being a bot or due to the message being a webhook
	 * message.
	 *
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html">Message</a>
	 *                which the bot received.
	 * @return {@code true} if the message is valid, {@code false} if the message is invalid.
	 */
	private boolean isValidMessage(final Message message) {
		if (message.getAuthor().isBot()) {
			return false;
		}

		return !message.isWebhookMessage();
	}

	/**
	 * Checks if the message uses the correct command prefix for the bot which is defined in the environment variables.
	 *
	 * @param cmdPrefix      The used command prefix by the bot.
	 * @param messageContent The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html#getContentRaw()">raw content of the Message</a>
	 *                       which the bot received.
	 * @return {@code true} if the prefix is valid, {@code false} if the prefix is invalid.
	 */
	private boolean isValidCommandPrefix(final String cmdPrefix, final String messageContent) {
		return messageContent.startsWith(cmdPrefix);
	}

	/**
	 * Checks if the author of the command has permission to use the command and if the <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 * is authorized for command usage.
	 *
	 * @param author  The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Member.html">Member</a>
	 *                that sent the message.
	 * @param channel The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                in which the message was sent.
	 * @return {@code true} if the channel is authorized and the author has an authorized role, {@code false} if the
	 * channel is unauthorized or if the author has no authorized
	 * <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Role.html">Role</a>.
	 * The author does not need an authorized role if he is the owner of the
	 * <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>.
	 */
	private boolean isAuthorizedUsage(final Member author, final TextChannel channel) {
		final Guild guild = channel.getGuild();
		final Optional<DiscordGuild> guildOptional = guildRepo.findById(guild.getIdLong());
		if (guildOptional.isEmpty()) {
			return false;
		}

		final long channelId = channel.getIdLong();
		if (!isChannelAuthorized(guild.getIdLong(), channelId)) {
			return false;
		}

		if (author.isOwner()) {
			return true;
		}

		final List<Role> authorRoles = author.getRoles();
		return isRoleAuthorized(guild.getIdLong(), authorRoles);
	}

	/**
	 * Checks if the channel the command got used in is authorized for command usage.
	 *
	 * @param guildId   The ID of the guild the message got send.
	 * @param channelId The ID of the channel the message got send in.
	 * @return {@code true} if the channel is authorized.
	 */
	private boolean isChannelAuthorized(long guildId, long channelId) {
		final Set<AuthedChannel> authedChannels = channelRepo.findAllByGuild_GuildId(guildId);
		if (authedChannels.size() == 0) {
			return true;
		}

		for (AuthedChannel authedChannel : authedChannels) {
			if (authedChannel.getChannelId() == channelId) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if a role of the user that used the command is authorized for command usage.
	 *
	 * @param guildId The ID of the guild the message got send.
	 * @param roles   The list of roles the user has.
	 * @return {@code true} if the user has an authorized role.
	 */
	private boolean isRoleAuthorized(final long guildId, List<Role> roles) {
		final Set<AuthedRole> authedRoles = roleRepo.findAllByGuild_GuildId(guildId);
		for (AuthedRole authedRole : authedRoles) {
			for (Role role : roles) {
				final long roleId = role.getIdLong();
				if (authedRole.getRoleId() == roleId) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Identifies the command name of the used command in the message.
	 *
	 * @param cmdPrefix      The used command prefix by the bot.
	 * @param messageContent The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html#getContentRaw()">raw content of the Message</a>
	 *                       which the bot received.
	 * @return the name of the used command in lower case.
	 */
	private String identifyCommandName(final String cmdPrefix, final String messageContent) {
		final String[] tokens = messageContent.split(" ");
		final String fullCommand = tokens[0];
		final String commandName = fullCommand.replace(cmdPrefix, "");
		return commandName.toLowerCase();        // lower case is needed for the matching to work in any case! DO NOT remove it!
	}

	/**
	 * Checks if the <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>
	 * has permission to use the command.
	 *
	 * @param dcGuild The Discord Guild in which the message was sent.
	 * @param command The information about the used command which contains the needed permissions to use it.
	 * @return {@code true} if the <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>
	 * has permission to use the command, {@code false} if the <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>
	 * lacks the needed permissions.
	 */
	private boolean isAuthorizedGuild(final DiscordGuild dcGuild, final Command command) {
		// if command needs read permission, but guild does not have it
		if (command.needsReadPerms() && !dcGuild.hasReadPerm()) {
			return false;
		}

		// if command needs write permission, but guild does not have it
		return !command.needsWritePerms() || dcGuild.hasWritePerm();
	}

	private boolean isCallerBotOwner(Member caller) {
		final JDA jda = caller.getJDA();
		final User owner = jda.retrieveApplicationInfo().complete().getOwner();
		return (owner.getIdLong() == caller.getIdLong());
	}

	/**
	 * Executes a command and handles exception if the bot does not have the needed permissions to
	 * execute that command in the channel/guild.
	 *
	 * @param event   The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">GuildMessageReceivedEvent</a>
	 *                provided by JDA.
	 * @param command The command to execute.
	 */
	private void executeCommand(final GuildMessageReceivedEvent event, final Command command) {
		try {
			command.execute(event);
		} catch (InsufficientPermissionException e) {
			String message = "Bot does not have the needed permission " + e.getPermission() + " for that command.";
			event.getChannel().sendMessage(message).queue();
		} catch (HierarchyException e) {
			String message = "Bot can not modify some of this users roles! Please move the bot role above any forum role.";
			event.getChannel().sendMessage(message).queue();
		}
	}

	/**
	 * Deletes the given message if it got sent in the verification channel of the guild. However, does not delete
	 * its own messages.
	 *
	 * @param dcGuild The Discord guild in which the message got sent.
	 * @param message The message to clear if it got sent in the verification channel.
	 */
	private void deleteInVerify(final DiscordGuild dcGuild, final Message message) {
		final TextChannel channel = message.getTextChannel();
		final User author = message.getAuthor();
		final Member self = message.getGuild().getSelfMember();
		if (author.getIdLong() == self.getIdLong()) {
			return;
		}

		if (isVerifyChannel(dcGuild, channel)) {
			deleteMessage(message);
		}
	}

	/**
	 * Checks if the channel is the verification channel of the guild the message got sent in.
	 *
	 * @param dcGuild The Discord guild in which the message got sent.
	 * @param channel The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                in which the message got sent.
	 * @return {@code true} if the channel is the verification channel of the guild, {@code false} if is not.
	 */
	private boolean isVerifyChannel(final DiscordGuild dcGuild, final TextChannel channel) {
		return dcGuild.getVerificationChannelId() == channel.getIdLong();
	}

	/**
	 * Gets the {@link DiscordGuild} for the given guild ID in the database or creates a new one if it does not exist yet.
	 *
	 * @param guildId The guild ID to check the database for.
	 * @return The {@link DiscordGuild} for the given ID.
	 */
	private DiscordGuild getDiscordGuild(final long guildId) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		return dcGuildOpt.orElseGet(() -> {
			final DiscordGuild newGuild = DiscordGuild.createDefault(guildId);
			guildRepo.save(newGuild);
			return newGuild;
		});
	}

	/**
	 * Deletes a given Discord message.
	 *
	 * @param message The message to delete.
	 */
	private void deleteMessage(final Message message) {
		message.delete().queue(
				v -> LogUtil.logDebug("Deleted non-command message in verification channel."),
				throwable -> LogUtil.logWarning("Could not delete message in verification channel! " + throwable.getMessage())
		);
	}
}
