package com.motorbesitzen.rolewatcher.data.dao;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;

@Entity
public class AuthedChannel {

	@Id
	@Min(value = 10000000000000000L)
	private long channelId;

	@ManyToOne
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	protected AuthedChannel() {
	}

	public AuthedChannel(long channelId, DiscordGuild guild) {
		this.channelId = channelId;
		this.guild = guild;
	}

	public long getChannelId() {
		return channelId;
	}

	public void setChannelId(long channelId) {
		this.channelId = channelId;
	}

	public DiscordGuild getGuild() {
		return guild;
	}

	public void setGuild(DiscordGuild guild) {
		this.guild = guild;
	}
}
