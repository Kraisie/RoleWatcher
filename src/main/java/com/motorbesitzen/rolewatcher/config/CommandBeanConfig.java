package com.motorbesitzen.rolewatcher.config;

import com.motorbesitzen.rolewatcher.bot.command.impl.*;
import com.motorbesitzen.rolewatcher.bot.service.ForumRoleApiRequest;
import com.motorbesitzen.rolewatcher.data.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class defines the bot commands as beans so Spring can control them and generate them when needed.
 * The method name like "help()" will result in the bean name "help" so make sure to name the method
 * as the lower case version of the command. A command like "?AuthoRize #channel" would need "authorize()"
 * as a method. Do not use camel case here, even though it is against the Java practice.
 * If the command needs access to a repository make sure to let Spring autowire it as parameters. This also
 * works for any other Spring controlled class.
 */
@Configuration
public class CommandBeanConfig {

	@Bean
	@Autowired
	AddRole addrole(final ForumRoleRepo forumRoleRepo) {
		return new AddRole(forumRoleRepo);
	}

	@Bean
	@Autowired
	AddUser adduser(final DiscordUserRepo discordUserRepo, final ForumUserRepo forumUserRepo, final ForumRoleRepo forumRoleRepo, final ForumRoleApiRequest forumRoleApiRequest) {
		return new AddUser(discordUserRepo, forumUserRepo, forumRoleRepo, forumRoleApiRequest);
	}

	@Bean
	@Autowired
	AddWhitelist addwhitelist(final DiscordUserRepo discordUserRepo) {
		return new AddWhitelist(discordUserRepo);
	}

	@Bean
	AllowVideo allowvideo() {
		return new AllowVideo();
	}

	@Bean
	Announce announce() {
		return new Announce();
	}

	@Bean
	@Autowired
	Authorize authorize(final AuthedChannelRepo channelRepo, final AuthedRoleRepo roleRepo, final DiscordGuildRepo discordGuildRepo) {
		return new Authorize(channelRepo, roleRepo, discordGuildRepo);
	}

	@Bean
	BlockVideo blockvideo() {
		return new BlockVideo();
	}

	@Bean
	@Autowired
	Deauthorize deauthorize(final AuthedChannelRepo channelRepo, final AuthedRoleRepo roleRepo) {
		return new Deauthorize(channelRepo, roleRepo);
	}

	@Bean
	@Autowired
	DeleteRole delrole(final ForumRoleRepo forumRoleRepo) {
		return new DeleteRole(forumRoleRepo);
	}

	@Bean
	@Autowired
	DeleteUser deluser(final ForumRoleRepo forumRoleRepo, final ForumUserRepo forumUserRepo) {
		return new DeleteUser(forumRoleRepo, forumUserRepo);
	}

	@Bean
	Help help(final DiscordGuildRepo discordGuildRepo) {
		return new Help(discordGuildRepo);
	}

	@Bean
	@Autowired
	ImportBans importbans(final DiscordGuildRepo guildRepo) {
		return new ImportBans(guildRepo);
	}

	@Bean
	@Autowired
	Info info(final DiscordGuildRepo guildRepo, final AuthedChannelRepo channelRepo, final AuthedRoleRepo roleRepo) {
		return new Info(guildRepo, channelRepo, roleRepo);
	}

	@Bean
	@Autowired
	RemoveWhitelist removewhitelist(final DiscordUserRepo discordUserRepo) {
		return new RemoveWhitelist(discordUserRepo);
	}

	@Bean
	@Autowired
	UpdateUser update(ForumUserRepo forumUserRepo, ForumRoleRepo forumRoleRepo, ForumRoleApiRequest forumRoleApiRequest) {
		return new UpdateUser(forumUserRepo, forumRoleRepo, forumRoleApiRequest);
	}

	@Bean
	@Autowired
	UpdateGuildPerms updateperms(DiscordGuildRepo guildRepo) {
		return new UpdateGuildPerms(guildRepo);
	}

	@Bean
	@Autowired
	Who who(final DiscordUserRepo discordUserRepo, final ForumUserRepo forumUserRepo) {
		return new Who(discordUserRepo, forumUserRepo);
	}
}
