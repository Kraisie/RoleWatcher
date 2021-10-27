package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface DiscordBanRepo extends CrudRepository<DiscordBan, Long> {
	boolean existsByBannedUser_DiscordId(long discordId);

	Optional<DiscordBan> findByBannedUser_DiscordId(long userId);
}
