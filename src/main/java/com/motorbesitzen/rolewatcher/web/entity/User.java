package com.motorbesitzen.rolewatcher.web.entity;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class User {

	@NotNull
	@Min(value = 1)
	private Long uid;

	@NotNull
	@NotBlank
	@Size(max = 100)
	private String username;

	@NotNull
	@Min(value = 10000000000000000L)
	private Long discordid;

	// Jackson
	private User() {

	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public long getDiscordid() {
		return discordid;
	}

	public void setDiscordid(long discordid) {
		this.discordid = discordid;
	}
}
