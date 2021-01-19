package com.motorbesitzen.rolewatcher.bot.event;

import com.motorbesitzen.rolewatcher.data.repo.AuthedChannelRepo;
import com.motorbesitzen.rolewatcher.data.repo.AuthedRoleRepo;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Handles text channel and role deletions. If a channel or role was authorized it gets removed
 * from the list of authorized channels/roles for that guild.
 */
@Service
public class AuthedDeletionListener extends ListenerAdapter {

	private final AuthedChannelRepo authedChannelRepo;
	private final AuthedRoleRepo authedRoleRepo;

	@Autowired
	public AuthedDeletionListener(final AuthedChannelRepo authedChannelRepo, final AuthedRoleRepo authedRoleRepo) {
		this.authedChannelRepo = authedChannelRepo;
		this.authedRoleRepo = authedRoleRepo;
	}

	/**
	 * Removes text channel form authorized channel list on channel deletion.
	 *
	 * @param event The deletion event triggered by Discord.
	 */
	@Override
	public void onTextChannelDelete(final TextChannelDeleteEvent event) {
		TextChannel deletedChannel = event.getChannel();
		if (authedChannelRepo.existsById(deletedChannel.getIdLong())) {
			authedChannelRepo.deleteById(deletedChannel.getIdLong());
		}
	}

	/**
	 * Removes role form authorized role list on role deletion.
	 *
	 * @param event The deletion event triggered by Discord.
	 */
	@Override
	public void onRoleDelete(final RoleDeleteEvent event) {
		Role role = event.getRole();
		if (authedRoleRepo.existsById(role.getIdLong())) {
			authedRoleRepo.deleteById(role.getIdLong());
		}
	}
}
