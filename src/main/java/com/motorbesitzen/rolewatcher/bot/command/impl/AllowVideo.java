package com.motorbesitzen.rolewatcher.bot.command.impl;

import com.motorbesitzen.rolewatcher.bot.command.CommandImpl;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.List;

/**
 * Clears a "deny" in the Discord permissions to allow video streaming for the mentioned members in all voice
 * channels the bot can access.
 * Does not remove the override even if all the permissions are the same as normal. Might lead to a lot of
 * 'dead' overrides that do not override anything anymore.
 */
public class AllowVideo extends CommandImpl {

	/**
	 * Clears the Discord permission override for a member to allow video streaming for the mentioned members.
	 * Assumes that the video streaming permissions got removed with the {@link com.motorbesitzen.rolewatcher.bot.command.impl.BlockVideo}
	 * before. Does not allow a user to stream if he does not have the permission, just clears the "deny" for
	 * the video streaming permission if it exists as an override for that member!
	 *
	 * @param event The received command message event.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final Message message = event.getMessage();
		final List<Member> mentionedMembers = message.getMentionedMembers();
		if (mentionedMembers.size() == 0) {
			sendErrorMessage(channel, "Please mention at least one user to remove the block of video streaming for.");
			return;
		}

		allowAllVideoPermissions(event.getGuild(), mentionedMembers);
		answer(channel, "Removed the denial of video streaming for the mentioned members.");
	}

	/**
	 * Updates the permissions for all current possibilities (voice channels) by clearing the category permissions for
	 * video streaming or the voice channel permissions directly if they do not belong to a category or are not synced to it.
	 *
	 * @param guild            The guild the command got issued on.
	 * @param mentionedMembers The mentioned members in the message.
	 */
	private void allowAllVideoPermissions(final Guild guild, final List<Member> mentionedMembers) {
		final List<Category> categories = guild.getCategories();
		for (Category category : categories) {
			allowMemberVideoPermissions(category, mentionedMembers);
		}

		/*
		 * Currently there is no way to check if a channel is synced to a parent
		 * reliably due to weird discord issues and no flag to define if a
		 * channel is synced. However, if that changes one does not need to
		 * update all voice channels anymore but only those that have no parent
		 * or those that have a parent but are not synced to it as all other channels
		 * get changed with the category they are synced to (above).
		 *
		 * Issue: https://github.com/DV8FromTheWorld/JDA/issues/1485
		 */
		final List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
		for (VoiceChannel voiceChannel : voiceChannels) {
			allowMemberVideoPermissions(voiceChannel, mentionedMembers);
		}
	}

	/**
	 * Clears the video streaming permission for a channel or a category for each mentioned member.
	 *
	 * @param channel The channel or category to update the permissions of.
	 * @param members The list of mentioned members.
	 */
	private void allowMemberVideoPermissions(final GuildChannel channel, final List<Member> members) {
		for (Member member : members) {
			allowMemberVideoPermission(channel, member);
		}
	}

	/**
	 * Clears the streaming permission for a channel or a category for a member if the bot can access that channel.
	 * Ignores any channel that it can not access.
	 *
	 * @param channel The channel or category to update the permissions of.
	 * @param member  A member to update the permissions in the channel of.
	 */
	private void allowMemberVideoPermission(final GuildChannel channel, final Member member) {
		try {
			PermissionOverrideAction override = channel.putPermissionOverride(member);
			override.clear(Permission.VOICE_STREAM).queue();
		} catch (MissingAccessException e) {
			// bot should not need access to everything, it should only update all channels it has access to
			LogUtil.logDebug("Bot can not access \"" + channel.getName() + "\" (" + channel.getId() + ") to modify its permissions.");
		}
	}
}