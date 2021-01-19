package com.motorbesitzen.rolewatcher.data.dao;


import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;

@Entity
public class AuthedRole {

	@Id
	@Min(value = 10000000000000000L)
	private long roleId;

	@ManyToOne
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	protected AuthedRole() {
	}

	public AuthedRole(long roleId, DiscordGuild guild) {
		this.roleId = roleId;
		this.guild = guild;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public DiscordGuild getGuild() {
		return guild;
	}

	public void setGuild(DiscordGuild guild) {
		this.guild = guild;
	}
}
