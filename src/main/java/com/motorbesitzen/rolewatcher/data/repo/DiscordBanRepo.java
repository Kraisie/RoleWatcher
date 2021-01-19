package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface DiscordBanRepo extends CrudRepository<DiscordBan, Long> {
	boolean existsByBannedUser_DiscordIdAndGuild_GuildId(long discordId, long guildId);

	Optional<DiscordBan> findDiscordBanByBannedUser_DiscordIdAndGuild_GuildId(long bannedUserId, long guildId);
}
