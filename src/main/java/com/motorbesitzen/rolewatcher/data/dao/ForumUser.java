package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.validator.constraints.Length;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class ForumUser {

	@Id
	@Min(value = 1)
	private long forumId;

	@NotNull
	@NotBlank
	@Length(max = 100)
	private String forumUsername;

	@OneToOne
	@JoinColumn(name = "discordId")
	private DiscordUser linkedDiscordUser;

	protected ForumUser() {
	}

	private ForumUser(long forumId, String forumUsername) {
		this.forumId = forumId;
		this.forumUsername = forumUsername;
	}

	private ForumUser(long forumId, String forumUsername, DiscordUser linkedDiscordUser) {
		this.forumId = forumId;
		this.forumUsername = forumUsername;
		this.linkedDiscordUser = linkedDiscordUser;
	}

	public static ForumUser create(long forumId, String forumUsername) {
		return new ForumUser(forumId, forumUsername);
	}

	public static ForumUser withLinkedDiscordUser(long forumId, String forumUsername, DiscordUser discordUser) {
		return new ForumUser(forumId, forumUsername, discordUser);
	}

	public long getForumId() {
		return forumId;
	}

	public void setForumId(long forumId) {
		this.forumId = forumId;
	}

	public String getForumUsername() {
		return forumUsername;
	}

	public void setForumUsername(String name) {
		this.forumUsername = name;
	}

	public DiscordUser getLinkedDiscordUser() {
		return linkedDiscordUser;
	}

	public void setLinkedDiscordUser(DiscordUser linkedDiscordUser) {
		this.linkedDiscordUser = linkedDiscordUser;
	}

	@Override
	public String toString() {
		return "ForumUser{" +
				"forumId=" + forumId +
				", forumUsername='" + forumUsername + '\'' +
				", linkedDiscordUser=" + linkedDiscordUser +
				'}';
	}
}
