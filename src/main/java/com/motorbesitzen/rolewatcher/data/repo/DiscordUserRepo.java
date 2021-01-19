package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import org.springframework.data.repository.CrudRepository;


public interface DiscordUserRepo extends CrudRepository<DiscordUser, Long> {

}
