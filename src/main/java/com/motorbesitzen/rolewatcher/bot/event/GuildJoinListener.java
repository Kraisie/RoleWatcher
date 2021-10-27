package com.motorbesitzen.rolewatcher.bot.event;

import com.motorbesitzen.rolewatcher.data.dao.DiscordGuild;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Handles guild joins by the bot and adds a guild entry if needed (new guild).
 */
@Service
public class GuildJoinListener extends ListenerAdapter {

	private final DiscordGuildRepo discordGuildRepo;

	@Autowired
	private GuildJoinListener(final DiscordGuildRepo discordGuildRepo) {
		this.discordGuildRepo = discordGuildRepo;
	}

	/**
	 * If the bot gets added to an unknown guild it creates a default database entry for it in the database.
	 *
	 * @param event The Discord event that the bot joined a guild.
	 */
	@Override
	public void onGuildJoin(final GuildJoinEvent event) {
		final Guild guild = event.getGuild();
		if (discordGuildRepo.existsById(guild.getIdLong())) {
			return;
		}

		final DiscordGuild dcGuild = DiscordGuild.createDefault(guild.getIdLong());
		discordGuildRepo.save(dcGuild);
		LogUtil.logInfo("Added \"" + guild.getName() + "\" (" + guild.getId() + ") to the database with default settings.");
	}
}