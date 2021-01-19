package com.motorbesitzen.rolewatcher.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiscordMessageUtilTest {

	private Member member1;
	private Member member2;
	private Member member3;
	private Message message;

	@BeforeEach
	void mockJdaObjects() {
		member1 = mock(Member.class);
		when(member1.getIdLong()).thenReturn(1234567890L);

		member2 = mock(Member.class);
		when(member1.getIdLong()).thenReturn(5555577777L);

		member3 = mock(Member.class);
		when(member1.getIdLong()).thenReturn(1212121212L);

		message = mock(Message.class);
	}

	@Test
	@DisplayName("should get single mentioned member from message")
	void testGetSingleMentionedMemberFromMessage() {
		List<Member> mentionedMembers = new ArrayList<>();
		mentionedMembers.add(member1);
		when(message.getMentionedMembers()).thenReturn(mentionedMembers);

		long mentionedMemberId = DiscordMessageUtil.getMentionedMemberId(message);

		assertThat(mentionedMemberId).isEqualTo(member1.getIdLong());
	}

	@Test
	@DisplayName("should get first mentioned member from message with multiple mentions")
	void testGetFirstMentionedMemberFromMessageWithMultipleMentions() {
		List<Member> mentionedMembers = new ArrayList<>();
		mentionedMembers.add(member1);
		mentionedMembers.add(member2);
		mentionedMembers.add(member3);
		when(message.getMentionedMembers()).thenReturn(mentionedMembers);

		long mentionedMemberId = DiscordMessageUtil.getMentionedMemberId(message);

		assertThat(mentionedMemberId).isEqualTo(member1.getIdLong());
	}

	@Test
	@DisplayName("should find and parse an ID in text if there are no mentioned members")
	void testGetMentionedRawIdOnNoMentions() {
		when(message.getMentionedMembers()).thenReturn(new ArrayList<>());
		when(message.getContentRaw()).thenReturn("prefix_command id 1234567");

		long mentionedMemberId = DiscordMessageUtil.getMentionedMemberId(message);

		assertThat(mentionedMemberId).isEqualTo(1234567L);
	}

	@Test
	@DisplayName("should find and parse first ID in text if there are no mentioned members but multiple IDs in text")
	void testGetFirstMentionedRawIdOnNoMentions() {
		when(message.getMentionedMembers()).thenReturn(new ArrayList<>());
		when(message.getContentRaw()).thenReturn("prefix_command id 1234567 \"abc\" 54321");

		long mentionedMemberId = DiscordMessageUtil.getMentionedMemberId(message);

		assertThat(mentionedMemberId).isEqualTo(1234567L);
	}

	@Test
	@DisplayName("should return -1 on negative ID in text and no mentions")
	void testGetNegativeMentionedRawIdOnNoMentions() {
		when(message.getMentionedMembers()).thenReturn(new ArrayList<>());
		when(message.getContentRaw()).thenReturn("prefix_command -1234567 \"abc\"");

		long mentionedMemberId = DiscordMessageUtil.getMentionedMemberId(message);

		assertThat(mentionedMemberId).isEqualTo(-1L);
	}

	@Test
	@DisplayName("should return -1 on no mentions and no ID in text")
	void testGetRawIdOnNoMentionsAndNoRawId() {
		when(message.getMentionedMembers()).thenReturn(new ArrayList<>());
		when(message.getContentRaw()).thenReturn("prefix_command \"abc\"");

		long mentionedMemberId = DiscordMessageUtil.getMentionedMemberId(message);

		assertThat(mentionedMemberId).isEqualTo(-1L);
	}
}
