package com.motorbesitzen.rolewatcher.util;

import net.dv8tion.jda.api.entities.Message;

/**
 * Helper functions for Discord messages.
 */
public final class DiscordMessageUtil {

	/**
	 * Get a mentioned member ID from a message. If the message has a mention it uses the ID of the first mentioned member.
	 * If there is no mention it checks for a numeric ID token and if there are multiple chooses the first one.
	 *
	 * @param message The Discord message object.
	 * @return If there is a member ID found it returns the ID as a {@code long}. If a raw ID exceeds the {@code Long}
	 * limits it returns -1 as well if there is no ID found in the message.
	 */
	public static long getMentionedMemberId(final Message message) {
		if (message.getMentionedMembers().size() != 0) {
			return message.getMentionedMembers().get(0).getIdLong();
		}

		return getMentionedRawId(message);
	}

	/**
	 * Searches the message for a 'raw' numeric ID.
	 *
	 * @param message The Discord message object.
	 * @return If there is a ID found it returns the ID as a {@code long}, if not or if it exceeds the {@code Long}
	 * limits it returns -1.
	 */
	public static long getMentionedRawId(final Message message) {
		final String rawMessage = message.getContentRaw();
		final String[] tokens = rawMessage.split(" ");
		for (String token : tokens) {
			if (token.matches("[0-9]+")) {
				return ParseUtil.safelyParseStringToLong(token);
			}
		}

		return -1;
	}
}
