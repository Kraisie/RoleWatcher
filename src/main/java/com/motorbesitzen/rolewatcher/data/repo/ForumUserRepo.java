package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ForumUserRepo extends CrudRepository<ForumUser, Long> {
	Optional<ForumUser> findByForumIdOrLinkedDiscordUser_DiscordId(long forumId, long discordId);

	Optional<ForumUser> findByLinkedDiscordUser_DiscordId(long discordId);
}
