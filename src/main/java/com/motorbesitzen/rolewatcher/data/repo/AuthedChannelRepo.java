package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.AuthedChannel;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

public interface AuthedChannelRepo extends CrudRepository<AuthedChannel, Long> {
	@NotNull Set<AuthedChannel> findAll();

	Set<AuthedChannel> findAllByGuild_GuildId(long guildId);
}
