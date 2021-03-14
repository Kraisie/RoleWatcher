package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("riddle")
public class Riddle extends CommandImpl {

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		if (mentionedChannels.size() == 0) {
			sendMessage(event.getChannel(), "Please mention a channel to announce the riddle in!");
			return;
		}

		final TextChannel channel = mentionedChannels.get(0);
		final EmbedBuilder embedBuilder = buildRiddle();

		sendMessage(channel, embedBuilder.build());
	}

	private EmbedBuilder buildRiddle() {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("RIDDLE")
				.setDescription("You have the chance to win a 1 Month sub of your choice by solving this riddle.")
				.setColor(getEmbedColor())
				.setFooter(
						"The riddles do not require special programs apart from a web browser and Discord. " +
								"Techy/nerdy knowledge might help you but it is not needed to solve the riddles. If a lot of " +
								"people are stuck hints might get posted in this channel. The winners name and every answer " +
								"will get logged; any abuse can be punished!"
				)
				.addField(
						"How do I win?", "You win by being the first one to solve all riddles. " +
								"If there are technical problems/bugs (multiple people get the winner message etc.) the log will define the winner.",
						false
				)
				.addField("What is the riddle?", "The first clue can be found in this message[.](https://rolewatcher.click)", false)
				.addField(
						"Can I take part in the riddle?",
						"Yes, only EO staff (Moderator and above) are excluded from claiming the price.",
						false
				);

		return eb;
	}
}
