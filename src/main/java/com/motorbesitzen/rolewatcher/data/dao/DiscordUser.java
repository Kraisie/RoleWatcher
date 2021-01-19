package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

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

	@OneToMany(mappedBy = "bannedUser", cascade = CascadeType.ALL)
	private Set<DiscordBan> bans;

	protected DiscordUser() {
	}

	private DiscordUser(long discordId, boolean whitelisted) {
		this.discordId = discordId;
		this.whitelisted = whitelisted;
		this.linkedForumUser = null;
		this.bans = new HashSet<>();
	}

	private DiscordUser(long discordId, ForumUser forumUser) {
		this.discordId = discordId;
		this.whitelisted = false;
		this.linkedForumUser = forumUser;
		this.bans = new HashSet<>();
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

	public Set<DiscordBan> getBans() {
		return bans;
	}

	public void setBans(Set<DiscordBan> bans) {
		this.bans = bans;
	}

	@Override
	public String toString() {
		return "DiscordUser{" +
				"discordId=" + discordId +
				", whitelisted=" + whitelisted +
				'}';
	}
}
