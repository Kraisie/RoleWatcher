package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.validator.constraints.Range;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
public class DiscordGuild {

	@Id
	@Min(value = 10000000000000000L)
	private long guildId;

	@NotNull
	@ColumnDefault("false")
	private boolean writePerm;

	@NotNull
	@ColumnDefault("false")
	private boolean readPerm;

	@NotNull
	@ColumnDefault("false")
	private boolean roleSyncPerm;

	@NotNull
	@ColumnDefault("false")
	private boolean autokick;

	@NotNull
	@ColumnDefault("24")
	@Range(min = 12, max = 168)
	private int autokickHourDelay;

	@OneToMany(mappedBy = "guild", cascade = CascadeType.ALL)
	private Set<AuthedChannel> authedChannels;

	@OneToMany(mappedBy = "guild", cascade = CascadeType.ALL)
	private Set<AuthedRole> authedRoles;

	@OneToMany(mappedBy = "guild", cascade = CascadeType.ALL)
	private Set<DiscordBan> bans;

	protected DiscordGuild() {
	}

	public DiscordGuild(long guildId, boolean writePerm, boolean readPerm, boolean autokick, int autokickHourDelay) {
		this.guildId = guildId;
		this.writePerm = writePerm;
		this.readPerm = readPerm;
		this.autokick = autokick;
		this.autokickHourDelay = autokickHourDelay;
		this.authedChannels = new HashSet<>();
		this.authedRoles = new HashSet<>();
		this.bans = new HashSet<>();
	}

	public static DiscordGuild createDefault(long guildId) {
		return new DiscordGuild(guildId, false, false, false, 24);
	}

	public long getGuildId() {
		return guildId;
	}

	public void setGuildId(long guildId) {
		this.guildId = guildId;
	}

	public boolean hasWritePerm() {
		return writePerm;
	}

	public void setWritePerm(boolean writePerm) {
		this.writePerm = writePerm;
	}

	public boolean hasReadPerm() {
		return readPerm;
	}

	public void setReadPerm(boolean readPerm) {
		this.readPerm = readPerm;
	}

	public boolean hasRoleSyncPerm() {
		return roleSyncPerm;
	}

	public void setRoleSyncPerm(boolean roleSyncPerm) {
		this.roleSyncPerm = roleSyncPerm;
	}

	public boolean canAutokick() {
		return autokick;
	}

	public void setAutokick(boolean autokick) {
		this.autokick = autokick;
	}

	public int getAutokickHourDelay() {
		return autokickHourDelay;
	}

	public void setAutokickHourDelay(int autokickHourDelay) {
		this.autokickHourDelay = autokickHourDelay;
	}

	public Set<AuthedChannel> getAuthedChannels() {
		return authedChannels;
	}

	public void setAuthedChannels(Set<AuthedChannel> authedChannels) {
		this.authedChannels = authedChannels;
	}

	public Set<AuthedRole> getAuthedRoles() {
		return authedRoles;
	}

	public void setAuthedRoles(Set<AuthedRole> authedRoles) {
		this.authedRoles = authedRoles;
	}

	public Set<DiscordBan> getBans() {
		return bans;
	}

	public void setBans(Set<DiscordBan> bans) {
		this.bans = bans;
	}

	public void addBans(Set<DiscordBan> bans) {
		this.bans.addAll(bans);
	}
}
