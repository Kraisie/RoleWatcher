package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.AuthedRole;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

public interface AuthedRoleRepo extends CrudRepository<AuthedRole, Long> {
	@NotNull Set<AuthedRole> findAll();

	@NotNull Set<AuthedRole> findAllByGuild_GuildId(long guildId);
}
