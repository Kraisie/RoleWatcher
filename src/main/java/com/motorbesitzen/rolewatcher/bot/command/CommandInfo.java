package com.motorbesitzen.rolewatcher.bot.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Contains information about all commands like the name, usage information, and needed permissions.
 * Case does not matter as {@link #getCommandInfoByName(String)} ignores the case.
 * A command that requires the write permission should also always need the read permission.
 */
public enum CommandInfo {

	ADD_ROLE {
		@Override
		public String getName() {
			return "addrole";
		}

		@Override
		public boolean needsWritePerms() {
			return true;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " id \"rolename\"";
		}

		@Override
		public String getDescription() {
			return "Adds a forum role to the database.";
		}
	}, ADD_USER {
		@Override
		public String getName() {
			return "adduser";
		}

		@Override
		public boolean needsWritePerms() {
			return true;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " @member \"username\" uid";
		}

		@Override
		public String getDescription() {
			return "Adds a link between a Discord and a forum user.";
		}
	}, ADD_WHITELIST {
		@Override
		public String getName() {
			return "addwhitelist";
		}

		@Override
		public boolean needsWritePerms() {
			return true;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (@member|discordid)";
		}

		@Override
		public String getDescription() {
			return "Whitelists a user so his roles do not get updated and he will not be kicked if autokick is enabled. " +
					"Can be used with a Discord tag or a Discord ID.";
		}
	}, AUTHORIZE {
		@Override
		public String getName() {
			return "authorize";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return false;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (#channel|@role)+";
		}

		@Override
		public String getDescription() {
			return "Authorizes a role for using commands or a channel to use commands in. Can be used with " +
					"multiple discord tags for channels and/or roles.";
		}
	}, DEAUTHORIZE {
		@Override
		public String getName() {
			return "deauthorize";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return false;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (#channel|@role)+";
		}

		@Override
		public String getDescription() {
			return "Deauthorizes a role from using commands or a channel from using commands in. Can be used with " +
					"multiple discord tags for channels and/or roles.";
		}
	}, DELETE_ROLE {
		@Override
		public String getName() {
			return "delrole";
		}

		@Override
		public boolean needsWritePerms() {
			return true;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " id";
		}

		@Override
		public String getDescription() {
			return "Deletes a forum role from the database.";
		}
	}, DELETE_USER {
		@Override
		public String getName() {
			return "deluser";
		}

		@Override
		public boolean needsWritePerms() {
			return true;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (@member|discordid|uid)";
		}

		@Override
		public String getDescription() {
			return "Removes a link between Discord and forum user. Can be used with a Discord tag, " +
					"a Discord ID or a forum ID.";
		}
	}, HELP {
		@Override
		public String getName() {
			return "help";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return false;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName();
		}

		@Override
		public String getDescription() {
			return "Shows a list of commands that can be used.";
		}
	}, INFO {
		@Override
		public String getName() {
			return "info";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return false;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName();
		}

		@Override
		public String getDescription() {
			return "Lists all authorized channels and roles and shows if the guild has read or write permissions to the database.";
		}
	}, IMPORT_BANS {
		@Override
		public String getName() {
			return "importbans";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " guildid";
		}

		@Override
		public String getDescription() {
			return "Imports all saved from another guild and bans matching users.";
		}
	}, REMOVE_WHITELIST {
		@Override
		public String getName() {
			return "removewhitelist";
		}

		@Override
		public boolean needsWritePerms() {
			return true;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (@member|discordid)";
		}

		@Override
		public String getDescription() {
			return "Removes a user from the whitelist. Can be used with a Discord tag or a Discord ID.";
		}
	}, UPDATE_USER {
		@Override
		public String getName() {
			return "update";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (@member|discordid|uid)";
		}

		@Override
		public String getDescription() {
			return "Forces an update of the roles of the user according to his forum roles.";
		}
	}, UPDATE_GUILD_PERMS {
		@Override
		public String getName() {
			return "updateperms";
		}

		@Override
		public boolean needsWritePerms() {
			// the guild permissions do not matter as only the owner can use it
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			// the guild permissions do not matter as only the owner can use it
			return false;
		}

		@Override
		public boolean needsOwnerPerms() {
			return true;
		}

		@Override
		public String getUsage() {
			return getName() + " guildid \"permission\" (true|false)";
		}

		@Override
		public String getDescription() {
			return "Updates the permissions of a guild.";
		}
	}, WHO {
		@Override
		public String getName() {
			return "who";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return true;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return getName() + " (@member|discordid|uid)";
		}

		@Override
		public String getDescription() {
			return "Shows information about a specific user. Can be used with a Discord tag, " +
					"a Discord ID or a forum ID.";
		}
	}, UNKNOWN_COMMAND {
		@Override
		public String getName() {
			return "unknown";
		}

		@Override
		public boolean needsWritePerms() {
			return false;
		}

		@Override
		public boolean needsReadPerms() {
			return false;
		}

		@Override
		public boolean needsOwnerPerms() {
			return false;
		}

		@Override
		public String getUsage() {
			return "Can not be used.";
		}

		@Override
		public String getDescription() {
			return "Represents an unknown command with no functionality.";
		}
	};

	/**
	 * Compares given String and command name to find a matching command while ignoring case.
	 *
	 * @param name The lower case name of the command to find.
	 * @return The {@code CommandInfo} of a command with a fitting name. If there is no match it returns
	 * {@link #UNKNOWN_COMMAND}.
	 */
	public static CommandInfo getCommandInfoByName(final String name) {
		final CommandInfo[] commandInfos = values();
		for (CommandInfo commandInfo : commandInfos) {
			if (commandInfo.getName().equalsIgnoreCase(name)) {
				return commandInfo;
			}
		}

		return UNKNOWN_COMMAND;
	}

	/**
	 * Provides a {@code List<CommandInfo>} of all available commands in alphabetical order. The {@link #UNKNOWN_COMMAND}
	 * will not be present in the returned list.
	 *
	 * @return An alphabetically sorted list of all commands.
	 */
	public static List<CommandInfo> getAllCommandInfos() {
		final CommandInfo[] commandInfos = values();
		List<CommandInfo> allCommandInfos = new ArrayList<>();
		for (CommandInfo commandInfo : commandInfos) {
			if (commandInfo != UNKNOWN_COMMAND) {
				allCommandInfos.add(commandInfo);
			}
		}

		allCommandInfos.sort(Comparator.comparing(CommandInfo::getName));
		return allCommandInfos;
	}

	/**
	 * Get the name of the command. Make sure to use lower case on new commands!
	 *
	 * @return the lower case name of the command.
	 */
	public abstract String getName();

	/**
	 * Check if the command needs write permissions to be used.
	 *
	 * @return {@code true} if the command needs the <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>
	 * to have write permissions.
	 */
	public abstract boolean needsWritePerms();

	/**
	 * Check if the command needs read permissions to be used.
	 *
	 * @return {@code true} if the command needs the <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>
	 * to have read permissions.
	 */
	public abstract boolean needsReadPerms();

	/**
	 * Check if the user who uses this command needs to be the owner of the bot.
	 *
	 * @return {@code true} if the command needs the caller to be set as owner of the bot.
	 */
	public abstract boolean needsOwnerPerms();

	/**
	 * Get a sample on how to use the command. Can be used in help commands.
	 *
	 * @return an example usage with all variables listed.
	 */
	public abstract String getUsage();

	/**
	 * Get a description about the command. Can be used in help commands.
	 *
	 * @return a short information text about what the command does.
	 */
	public abstract String getDescription();
}
