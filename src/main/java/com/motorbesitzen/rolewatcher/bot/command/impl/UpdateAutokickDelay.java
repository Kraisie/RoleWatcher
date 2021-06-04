package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.DiscordMessageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Updates a guilds' autokick delay in Discord.
 */
@Service("updatedelay")
class UpdateAutokickDelay extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	UpdateAutokickDelay(DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "updatedelay";
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
		return getName() + " (12-168)";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Sets the autokick delay to an amount of hours between 12 and 168 (7 days).";
	}

	/**
	 * Updates the autokick delay for the guild the command got used in.
	 *
	 * @param event The Discord event triggered when a message is received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final long newDelay = DiscordMessageUtil.getMentionedRawId(message);
		if (newDelay < 12 || newDelay > 168) {
			sendErrorMessage(event.getChannel(), "Please provide a delay between 12 and 168 hours.");
			return;
		}

		final TextChannel channel = event.getChannel();
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> updateDelay(channel, dcGuild, (int) newDelay),
				() -> {
					final DiscordGuild newGuild = DiscordGuild.createDefault(guildId);
					updateDelay(channel, newGuild, (int) newDelay);
				}
		);
	}

	private void updateDelay(final TextChannel channel, final DiscordGuild dcGuild, final int delay) {
		dcGuild.setAutokickHourDelay(delay);
		guildRepo.save(dcGuild);
		answer(channel, "Updated the autokick delay to " + delay + "h for " + dcGuild.getGuildId() + ".");
	}
}
