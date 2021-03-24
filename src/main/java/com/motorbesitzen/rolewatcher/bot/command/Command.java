package com.motorbesitzen.rolewatcher.bot.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;

/**
 * The interface for any Command the bot can handle.
 */
public interface Command {

	/**
	 * Get the name of the command. The name should be in lower case and should be equal to the service name.
	 *
	 * @return The name of the command.
	 */
	String getName();

	/**
	 * Shows if the guild needs the 'write' permission to execute this command.
	 *
	 * @return {@code true} if the guild needs the 'write' permission.
	 */
	boolean needsWritePerms();

	/**
	 * Shows if the guild needs the 'read' permission to execute this command.
	 * Should also be {@code true} when {@link #needsWritePerms()} is {@code true}.
	 *
	 * @return {@code true} if the guild needs the 'read' permission.
	 */
	boolean needsReadPerms();

	/**
	 * Shows if the command can only be used by the owner of the bot.
	 *
	 * @return {@code true} if only the bot owner can use the command.
	 */
	boolean needsOwnerPerms();

	/**
	 * Displays the syntax for the command by defining the name and any additionally needed parameters.
	 *
	 * @return a representation on how to use the command
	 */
	String getUsage();

	/**
	 * Describes what the command does and includes any information that may be needed.
	 *
	 * @return a short text that describes the command and its functionality.
	 */
	String getDescription();

	/**
	 * A method that performs the necessary actions for the given command.
	 *
	 * @param event The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">Discord event</a>
	 *              when a message (possible command) is received.
	 */
	void execute(final GuildMessageReceivedEvent event);

	/**
	 * Sends an answer to a channel. Does not do anything different than {@link #sendMessage(TextChannel, String)} but
	 * clarifies that the message will be send as an answer to a command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	void answer(final TextChannel channel, final String message);

	/**
	 * Sends an embedded message as answer to a channel. Does not do anything different than
	 * {@link #sendMessage(TextChannel, MessageEmbed)} but clarifies that the message will be send as an answer to a
	 * command in the caller channel.
	 *
	 * @param channel      <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     to send the message in.
	 * @param embedBuilder The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                     to send as answer.
	 */
	void answer(final TextChannel channel, final MessageEmbed embedBuilder);

	/**
	 * Sends a message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	void sendMessage(final TextChannel channel, final String message);

	/**
	 * Sends an embedded message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel      <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     to send the message in.
	 * @param embedBuilder The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                     to send as answer.
	 */
	void sendMessage(final TextChannel channel, final MessageEmbed embedBuilder);

	/**
	 * Sends a placeholder message which can be updated e.g. when a task succeeds. Does not send a message if the bot
	 * has no permissions to write in the given chat.
	 *
	 * @param channel            <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                           to send the placeholder in.
	 * @param placeholderMessage The message content so send as a placeholder.
	 * @return The Discord <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html">Message</a>
	 * object of the sent message, {@code null} if the bot can not write in the given channel.
	 */
	Message answerPlaceholder(final TextChannel channel, final String placeholderMessage);

	/**
	 * Edits a given Discord Message objects message. Sends error message in channel if given message is not written
	 * by the bot. Does not do anything if message does not exist anymore or if the bot does not have the needed
	 * permissions.
	 *
	 * @param message    The Discord <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html">Message</a>
	 *                   object that is supposed to get edited.
	 * @param newMessage The new message content for the Discord Message.
	 */
	void editPlaceholder(final Message message, final String newMessage);

	/**
	 * Edits a message in a channel by ID. Sends an error message with the new content if ID does not exist or if
	 * the given message is not written by the bot. Does not do anything if the bot does not have the needed permissions.
	 *
	 * @param channel    The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                   where the original message is located in.
	 * @param messageId  The message ID of the original message.
	 * @param newMessage The new content for the message.
	 */
	void editPlaceholder(final TextChannel channel, final long messageId, final String newMessage);

	/**
	 * Used to clarify in the code that an error message is sent, doesn't do anything else than a normal answer message.
	 *
	 * @param channel      The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     where the original message is located in.
	 * @param errorMessage The error message to send.
	 */
	void sendErrorMessage(final TextChannel channel, final String errorMessage);

	/**
	 * Defines the color used for embeds in Discord (color bar on the left). If no color is set in the environment
	 * variables it returns orange ({@code #de690c} / {@code rgb(222,105,12)}).
	 *
	 * @return The color to use for embeds.
	 */
	Color getEmbedColor();
}
