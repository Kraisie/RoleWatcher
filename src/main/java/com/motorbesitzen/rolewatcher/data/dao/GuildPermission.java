package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class GuildPermission {

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
	private boolean banSyncPerm;

	@NotNull
	@ColumnDefault("false")
	private boolean autokick;

	protected GuildPermission() {

	}

	private GuildPermission(boolean writePerm, boolean readPerm, boolean roleSyncPerm, boolean banSyncPerm, boolean autokick) {
		this.writePerm = writePerm;
		this.readPerm = readPerm;
		this.roleSyncPerm = roleSyncPerm;
		this.banSyncPerm = banSyncPerm;
		this.autokick = autokick;
	}

	static GuildPermission allOff() {
		return new GuildPermission();
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

	public boolean hasBanSyncPerm() {
		return banSyncPerm;
	}

	public void setBanSyncPerm(boolean banSyncPerm) {
		this.banSyncPerm = banSyncPerm;
	}

	public boolean canAutokick() {
		return autokick;
	}

	public void setAutokick(boolean autokick) {
		this.autokick = autokick;
	}
}
