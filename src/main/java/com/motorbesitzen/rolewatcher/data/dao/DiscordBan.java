package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"guildId", "bannedDiscordUserId"}))
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
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	@ManyToOne
	@JoinColumn(name = "bannedDiscordUserId")
	private DiscordUser bannedUser;

	protected DiscordBan() {
	}

	private DiscordBan(long actorDiscordId, String reason, DiscordGuild guild, DiscordUser bannedUser) {
		this.actorDiscordId = actorDiscordId;
		this.reason = reason;
		this.guild = guild;
		this.bannedUser = bannedUser;
	}

	public static DiscordBan createDiscordBan(long actorDiscordId, String reason, DiscordGuild guild, DiscordUser bannedUser) {
		return new DiscordBan(actorDiscordId, reason, guild, bannedUser);
	}

	public static DiscordBan withUnknownActor(String reason, DiscordGuild guild, DiscordUser bannedUser) {
		return new DiscordBan(0, reason, guild, bannedUser);
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

	public DiscordGuild getGuild() {
		return guild;
	}

	public void setGuild(DiscordGuild guild) {
		this.guild = guild;
	}

	public DiscordUser getBannedUser() {
		return bannedUser;
	}

	public void setBannedUser(DiscordUser bannedUser) {
		this.bannedUser = bannedUser;
	}
}
