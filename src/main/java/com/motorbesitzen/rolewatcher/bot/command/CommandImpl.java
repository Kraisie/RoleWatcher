package com.motorbesitzen.rolewatcher.bot.command;

import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.util.ParseUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.awt.*;

/**
 * Basic implementation of a Command. Has all needed methods to send messages, answer to commands and log (debug) actions.
 * All subclasses (Commands) can use these functions.
 */
@Service
public class CommandImpl implements Command {

	/**
	 * {@inheritDoc}
	 * Placeholder for subclass implementation, does not do anything as this class is not a command which the bot
	 * handles.
	 */
	@Override
	public void execute(GuildMessageReceivedEvent event) {
		// Perform tasks in subclasses, not here!
		LogUtil.logDebug("");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void answer(TextChannel channel, String message) {
		sendMessage(channel, message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void answer(TextChannel channel, MessageEmbed message) {
		sendMessage(channel, message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessage(TextChannel channel, String message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessage(TextChannel channel, MessageEmbed message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Message answerPlaceholder(TextChannel channel, String placeholderMessage) {
		if (!channel.canTalk()) {
			return null;
		}

		return channel.sendMessage(placeholderMessage).complete();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void editPlaceholder(Message message, String newMessage) {
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
	 * {@inheritDoc}
	 */
	@Override
	public void editPlaceholder(TextChannel channel, long messageId, String newMessage) {
		if (!channel.canTalk()) {
			return;
		}

		Message message = channel.getHistory().getMessageById(messageId);
		if (message == null) {
			sendErrorMessage(
					channel, "Can not edit message!\nMessage ID " + messageId + " not found in " +
							channel.getAsMention() + ".\n New message: \"" + newMessage + "\""
			);
			return;
		}

		editPlaceholder(message, newMessage);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendErrorMessage(TextChannel channel, String errorMessage) {
		answer(channel, errorMessage);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Color getEmbedColor() {
		String envR = EnvironmentUtil.getEnvironmentVariableOrDefault("EMBED_COLOR_R", "222");
		String envG = EnvironmentUtil.getEnvironmentVariableOrDefault("EMBED_COLOR_G", "105");
		String envB = EnvironmentUtil.getEnvironmentVariableOrDefault("EMBED_COLOR_B", "12");

		int r = ParseUtil.safelyParseStringToInt(envR);
		int g = ParseUtil.safelyParseStringToInt(envG);
		int b = ParseUtil.safelyParseStringToInt(envB);

		// make sure r,g,b stay in rgb range of 0-255
		return new Color(
				Math.max(0, r % 256),
				Math.max(0, g % 256),
				Math.max(0, b % 256)
		);
	}
}
