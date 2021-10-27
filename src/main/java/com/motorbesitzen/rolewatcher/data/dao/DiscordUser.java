package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity
public class DiscordUser {

	@Id
	@Min(value = 10000000000000000L)
	private long discordId;

	@NotNull
	@ColumnDefault("false")
	private boolean whitelisted;

	@OneToOne(mappedBy = "linkedDiscordUser", cascade = CascadeType.ALL)
	private ForumUser linkedForumUser;

	@OneToOne(mappedBy = "bannedUser", cascade = CascadeType.ALL)
	private DiscordBan ban;

	protected DiscordUser() {
	}

	private DiscordUser(long discordId, boolean whitelisted) {
		this.discordId = discordId;
		this.whitelisted = whitelisted;
		this.linkedForumUser = null;
		this.ban = null;
	}

	private DiscordUser(long discordId, ForumUser forumUser) {
		this.discordId = discordId;
		this.whitelisted = false;
		this.linkedForumUser = forumUser;
		this.ban = null;
	}

	public static DiscordUser createDiscordUser(long discordId) {
		return new DiscordUser(discordId, false);
	}

	public static DiscordUser createWhitelistedDiscordUser(long discordId) {
		return new DiscordUser(discordId, true);
	}

	public static DiscordUser createLinkedDiscordUser(long discordId, ForumUser forumUser) {
		return new DiscordUser(discordId, forumUser);
	}

	public long getDiscordId() {
		return discordId;
	}

	public void setDiscordId(long discordId) {
		this.discordId = discordId;
	}

	public boolean isWhitelisted() {
		return whitelisted;
	}

	public void setWhitelisted(boolean whitelisted) {
		this.whitelisted = whitelisted;
	}

	public ForumUser getLinkedForumUser() {
		return linkedForumUser;
	}

	public void setLinkedForumUser(ForumUser linkedForumUser) {
		this.linkedForumUser = linkedForumUser;
	}

	public DiscordBan getBan() {
		return ban;
	}

	public void setBan(DiscordBan ban) {
		this.ban = ban;
	}

	@Override
	public String toString() {
		return "DiscordUser{" +
				"discordId=" + discordId +
				", whitelisted=" + whitelisted +
				'}';
	}
}
