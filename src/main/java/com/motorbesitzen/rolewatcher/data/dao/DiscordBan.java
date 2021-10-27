package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class DiscordBan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long banId;

	@Min(value = 0)
	private long actorDiscordId;

	@NotNull
	@ColumnDefault("'No reason given.'")
	@NotBlank
	private String reason;

	@ManyToOne
	@JoinColumn(name = "bannedDiscordUserId")
	private DiscordUser bannedUser;

	protected DiscordBan() {
	}

	private DiscordBan(long actorDiscordId, String reason, DiscordUser bannedUser) {
		this.actorDiscordId = actorDiscordId;
		this.reason = reason;
		this.bannedUser = bannedUser;
	}

	public static DiscordBan createDiscordBan(long actorDiscordId, String reason, DiscordUser bannedUser) {
		return new DiscordBan(actorDiscordId, reason, bannedUser);
	}

	public static DiscordBan withUnknownActor(String reason, DiscordUser bannedUser) {
		return new DiscordBan(0, reason, bannedUser);
	}

	public long getBanId() {
		return banId;
	}

	public void setBanId(long id) {
		this.banId = id;
	}

	public long getActorDiscordId() {
		return actorDiscordId;
	}

	public void setActorDiscordId(long actorDiscordId) {
		this.actorDiscordId = actorDiscordId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public DiscordUser getBannedUser() {
		return bannedUser;
	}

	public void setBannedUser(DiscordUser bannedUser) {
		this.bannedUser = bannedUser;
	}

	@Override
	public String toString() {
		return '{' +
				"banId=" + banId +
				", actorDiscordId=" + actorDiscordId +
				", reason='" + reason + '\'' +
				", bannedUser=" + bannedUser +
				'}';
	}
}
