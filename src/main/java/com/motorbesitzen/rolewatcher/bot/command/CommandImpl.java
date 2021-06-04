package com.motorbesitzen.rolewatcher.bot.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Basic implementation of a Command. Has all needed methods to send messages, answer to commands and log (debug) actions.
 * All subclasses (Commands) can use these functions.
 */
@Service
public abstract class CommandImpl implements Command {

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract String getName();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean needsAuthorization();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean needsWritePerms();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean needsReadPerms();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean needsOwnerPerms();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract String getUsage();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract String getDescription();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract void execute(final GuildMessageReceivedEvent event);

	/**
	 * Sends an answer to a channel. Does not do anything different than {@link #sendMessage(TextChannel, String)} but
	 * clarifies that the message will be send as an answer to a command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void answer(final TextChannel channel, final String message) {
		sendMessage(channel, message);
	}

	/**
	 * Sends an embedded message as answer to a channel. Does not do anything different than
	 * {@link #sendMessage(TextChannel, MessageEmbed)} but clarifies that the message will be send as an answer to a
	 * command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                to send as answer.
	 */
	protected void answer(final TextChannel channel, final MessageEmbed message) {
		sendMessage(channel, message);
	}

	/**
	 * Sends a message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void sendMessage(final TextChannel channel, final String message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * Sends an embedded message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                to send as answer.
	 */
	protected void sendMessage(final TextChannel channel, final MessageEmbed message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

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
	protected Message answerPlaceholder(final TextChannel channel, final String placeholderMessage) {
		if (!channel.canTalk()) {
			return null;
		}

		return channel.sendMessage(placeholderMessage).complete();
	}

	/**
	 * Edits a given Discord Message objects message. Sends error message in channel if given message is not written
	 * by the bot. Does not do anything if message does not exist anymore or if the bot does not have the needed
	 * permissions.
	 *
	 * @param message    The Discord <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html">Message</a>
	 *                   object that is supposed to get edited.
	 * @param newMessage The new message content for the Discord Message.
	 */
	protected void editPlaceholder(final Message message, final String newMessage) {
		if (!message.getTextChannel().canTalk()) {
			return;
		}

		try {
			message.editMessage(newMessage).queue();
		} catch (IllegalStateException e) {
			sendErrorMessage(
					message.getTextChannel(),
					"Can not edit message (" + message.getId() + ") from another user!"
			);
		}
	}

	/**
	 * Edits a message in a channel by ID. Sends an error message with the new content if ID does not exist or if
	 * the given message is not written by the bot. Does not do anything if the bot does not have the needed permissions.
	 *
	 * @param channel    The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                   where the original message is located in.
	 * @param messageId  The message ID of the original message.
	 * @param newMessage The new content for the message.
	 */
	protected void editPlaceholder(final TextChannel channel, final long messageId, final String newMessage) {
		if (!channel.canTalk()) {
			return;
		}

		channel.retrieveMessageById(messageId).queue(
				message -> editPlaceholder(message, newMessage),
				throwable -> sendErrorMessage(
						channel, "Can not edit message!\nMessage ID " + messageId + " not found in " +
								channel.getAsMention() + ".\n New message: \"" + newMessage + "\""
				)
		);
	}

	/**
	 * Used to clarify in the code that an error message is sent, doesn't do anything else than a normal answer message.
	 *
	 * @param channel      The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     where the original message is located in.
	 * @param errorMessage The error message to send.
	 */
	protected void sendErrorMessage(final TextChannel channel, final String errorMessage) {
		answer(channel, errorMessage);
	}

	/**
	 * Sends a temporary error messages that gets deleted after a certain time has passed.
	 *
	 * @param channel         <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                        to send the error message in.
	 * @param errorMessage    The error message to send.
	 * @param deleteTimerSecs The time in seconds until the error message should get deleted.
	 */
	protected void sendTemporaryErrorMessage(final TextChannel channel, final String errorMessage, final int deleteTimerSecs) {
		sendTemporaryMessage(channel, errorMessage, deleteTimerSecs);
	}

	/**
	 * Sends a temporary messages that gets deleted after a certain time has passed.
	 *
	 * @param channel         <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                        to send the message in.
	 * @param message         The message to send.
	 * @param deleteTimerSecs The time in seconds until the message should get deleted.
	 */
	protected void sendTemporaryMessage(final TextChannel channel, final String message, final int deleteTimerSecs) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue(
					msg -> msg.delete().queueAfter(deleteTimerSecs, TimeUnit.SECONDS)
			);
		}
	}
}
