package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("setverify")
public class VerifyChannel extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private VerifyChannel(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "setverify";
	}

	@Override
	public boolean needsAuthorization() {
		return true;
	}

	@Override
	public boolean needsWritePerms() {
		return false;
	}

	@Override
	public boolean needsReadPerms() {
		return false;
	}

	@Override
	public boolean needsOwnerPerms() {
		return false;
	}

	@Override
	public String getUsage() {
		return getName() + " #channel";
	}

	@Override
	public String getDescription() {
		return "Sets a channel as verification channel where users can link their forum and Discord accounts." +
				"Use an ID of 0 to set no verification channel.";
	}

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		if (mentionedChannels.size() == 0) {
			sendErrorMessage(event.getChannel(), "Please mention a channel to use as the verification channel.");
			return;
		}

		final TextChannel verificationChannel = mentionedChannels.get(0);
		final long verificationChannelId = verificationChannel.getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.createDefault(guildId));
		dcGuild.setVerificationChannelId(verificationChannelId);
		guildRepo.save(dcGuild);
		answer(event.getChannel(), "Set " + verificationChannel.getAsMention() + " as the verification channel.");
	}
}
