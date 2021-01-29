package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.data.repo.AuthedChannelRepo;
import com.motorbesitzen.rolewatcher.data.repo.AuthedRoleRepo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Used to deauthorize Discord text channels and Discord roles to use bot commands.
 */
@Service("deauthorize")
public class Deauthorize extends CommandImpl {

	private final AuthedChannelRepo channelRepo;
	private final AuthedRoleRepo roleRepo;

	@Autowired
	public Deauthorize(final AuthedChannelRepo channelRepo, final AuthedRoleRepo roleRepo) {
		this.channelRepo = channelRepo;
		this.roleRepo = roleRepo;
	}

	/**
	 * Deauthorizes mentioned roles and channels in a message. If no roles or channels are mentioned
	 * it sends an error message in the chat where the command got used telling the user to mention
	 * a role or a channel. If none of the mentioned channels/roles is authorized it still replies with
	 * the message that the channels/roles got deauthorized!
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final TextChannel channel = event.getChannel();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		final List<Role> mentionedRoles = message.getMentionedRoles();
		if (mentionedChannels.size() == 0 && mentionedRoles.size() == 0) {
			sendErrorMessage(event.getChannel(), "Please provide one or more channels and/or roles to deauthorize for command usage.");
			return;
		}

		if (mentionedChannels.size() != 0) {
			deauthorizeChannels(mentionedChannels);
			answer(channel, "Deauthorized the mentioned channel(s).");
		}

		if (mentionedRoles.size() != 0) {
			deauthorizeRoles(mentionedRoles);
			answer(channel, "Deauthorized the mentioned role(s).");
		}
	}

	/**
	 * Authorizes mentioned channels if they are authorized.
	 *
	 * @param channels The mentioned channels in the command message.
	 */
	private void deauthorizeChannels(final List<TextChannel> channels) {
		for (TextChannel channel : channels) {
			if (!channelRepo.existsById(channel.getIdLong())) {
				continue;
			}

			channelRepo.deleteById(channel.getIdLong());
		}
	}

	/**
	 * Deauthorizes mentioned roles if they are authorized.
	 *
	 * @param roles The mentioned roles in the command message.
	 */
	private void deauthorizeRoles(final List<Role> roles) {
		for (Role role : roles) {
			if (!roleRepo.existsById(role.getIdLong())) {
				continue;
			}

			roleRepo.deleteById(role.getIdLong());
		}
	}
}
