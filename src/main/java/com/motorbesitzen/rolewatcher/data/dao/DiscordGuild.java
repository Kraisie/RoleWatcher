package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.validator.constraints.Range;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
public class DiscordGuild {

	@Id
	@Min(value = 10000000000000000L)
	private long guildId;

	@Embedded
	private GuildPermission perms;

	@ColumnDefault("0")
	private long verificationChannelId;

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

	public DiscordGuild(long guildId, GuildPermission perms, long verificationChannelId, int autokickHourDelay) {
		this.guildId = guildId;
		this.perms = perms;
		this.verificationChannelId = verificationChannelId;
		this.autokickHourDelay = autokickHourDelay;
		this.authedChannels = new HashSet<>();
		this.authedRoles = new HashSet<>();
		this.bans = new HashSet<>();
	}

	public static DiscordGuild createDefault(long guildId) {
		return new DiscordGuild(guildId, GuildPermission.allOff(), 0, 24);
	}

	public long getGuildId() {
		return guildId;
	}

	public void setGuildId(long guildId) {
		this.guildId = guildId;
	}

	public boolean hasWritePerm() {
		return perms.hasWritePerm();
	}

	public void setWritePerm(boolean writePerm) {
		this.perms.setWritePerm(writePerm);
	}

	public boolean hasReadPerm() {
		return perms.hasReadPerm();
	}

	public void setReadPerm(boolean readPerm) {
		this.perms.setReadPerm(readPerm);
	}

	public boolean hasRoleSyncPerm() {
		return perms.hasRoleSyncPerm();
	}

	public void setRoleSyncPerm(boolean roleSyncPerm) {
		this.perms.setRoleSyncPerm(roleSyncPerm);
	}

	public boolean hasBanSyncPerm() {
		return perms.hasBanSyncPerm();
	}

	public void setBanSyncPerm(boolean banSyncPerm) {
		this.perms.setBanSyncPerm(banSyncPerm);
	}

	public boolean canAutokick() {
		return perms.canAutokick();
	}

	public void setAutokick(boolean autokick) {
		this.perms.setAutokick(autokick);
	}

	public long getVerificationChannelId() {
		return verificationChannelId;
	}

	public void setVerificationChannelId(long verificationChannelId) {
		this.verificationChannelId = verificationChannelId;
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

	public String getPermissionText() {
		return "Read: " + (hasReadPerm() ? "Yes" : "No") + "\n" +
				"Write: " + (hasWritePerm() ? "Yes" : "No") + "\n" +
				"RoleSync: " + (hasRoleSyncPerm() ? "Yes" : "No");
	}
}
