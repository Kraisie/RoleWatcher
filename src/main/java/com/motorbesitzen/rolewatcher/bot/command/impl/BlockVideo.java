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
 * Overrides the Discord permissions to block the mentioned members from streaming screen content or via webcam in
 * all voice channels the bot can access.
 */
public class BlockVideo extends CommandImpl {

	/**
	 * Overrides the Discord permissions to block the mentioned members from streaming screen content or via webcam.
	 *
	 * @param event The received command message event.
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final Message message = event.getMessage();
		final List<Member> mentionedMembers = message.getMentionedMembers();
		if (mentionedMembers.size() == 0) {
			sendErrorMessage(channel, "Please mention at least one user to block their video permissions.");
			return;
		}

		blockAllVideoPermissions(event.getGuild(), mentionedMembers);
		answer(channel, "Blocked video permissions for the mentioned members.");
	}

	/**
	 * Updates the permissions for all current possibilities (voice channels) by updating the category permissions
	 * and the voice channel permissions.
	 *
	 * @param guild            The guild the command got issued on.
	 * @param mentionedMembers THe mentioned members in the message.
	 */
	private void blockAllVideoPermissions(final Guild guild, final List<Member> mentionedMembers) {
		final List<Category> categories = guild.getCategories();
		for (Category category : categories) {
			blockMemberVideoPermissions(category, mentionedMembers);
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
			blockMemberVideoPermissions(voiceChannel, mentionedMembers);
		}
	}

	/**
	 * Denies the permission for a channel or a category for each mentioned member.
	 *
	 * @param channel The channel or category to update the permissions of.
	 * @param members The list of mentioned members.
	 */
	private void blockMemberVideoPermissions(final GuildChannel channel, final List<Member> members) {
		for (Member member : members) {
			blockMemberVideoPermission(channel, member);
		}
	}

	/**
	 * Denies the streaming permission for a channel or a category for a member if the bot can access that channel.
	 * Ignores any channel that it can not access.
	 *
	 * @param channel The channel or category to update the permissions of.
	 * @param member  A member to update the permissions in the channel of.
	 */
	private void blockMemberVideoPermission(final GuildChannel channel, final Member member) {
		try {
			PermissionOverrideAction override = channel.putPermissionOverride(member);
			override.deny(Permission.VOICE_STREAM).queue();
		} catch (MissingAccessException e) {
			// bot should not need access to everything, it should only update all channels it has access to
			LogUtil.logDebug("Bot can not access \"" + channel.getName() + "\" (" + channel.getId() + ") to modify its permissions.");
		}
	}
}