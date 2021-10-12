package com.motorbesitzen.rolewatcher.util;

import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RoleUtil {

	/**
	 * Updates the Discord roles of a member according to the roles the user has on the forum.
	 * Removes forum roles the user does not have assigned anymore and adds forum roles that are missing.
	 *
	 * @param member           The member to update the roles of.
	 * @param memberForumRoles The roles the user has on the forum.
	 * @param allForumRoles    A list of all forum roles.
	 */
	public static void updateRoles(Member member, List<ForumRole> memberForumRoles, Iterable<ForumRole> allForumRoles) {
		Guild guild = member.getGuild();
		Member me = guild.getSelfMember();
		List<Role> rolesToAdd = new ArrayList<>();
		List<Role> rolesToRemove = new ArrayList<>();
		handleDuplicateNamedRoles(memberForumRoles, allForumRoles);

		for (ForumRole forumRole : allForumRoles) {
			List<Role> matchingRoles = guild.getRolesByName(forumRole.getRoleName(), true);
			if (matchingRoles.size() == 0) {
				continue;
			}

			Role matchingRole = matchingRoles.get(0);
			if (!me.canInteract(matchingRole)) {
				LogUtil.logWarning("Can not assign role \"" + matchingRole.getName() + "\" to members. Move bot role above that role!");
				continue;
			}

			if (memberForumRoles.contains(forumRole)) {
				rolesToAdd.add(matchingRole);
			} else {
				rolesToRemove.add(matchingRole);
			}
		}

		LogUtil.logDebug("CurrentRoles: " + Arrays.toString(member.getRoles().toArray()));
		LogUtil.logDebug("RolesAdd: " + Arrays.toString(rolesToAdd.toArray()));
		LogUtil.logDebug("RolesRemove: " + Arrays.toString(rolesToRemove.toArray()));

		addRoles(member, rolesToAdd);
		removeRoles(member, rolesToRemove);
	}

	/**
	 * If there are two roles with the same name but different IDs then just add that role to the users' roles.
	 * So the role does not get removed in Discord as {@code guild.modifyMemberRoles(...)} removes the role
	 * if it is present in the add and remove list.
	 *
	 * @param memberForumRoles The roles of the member.
	 * @param allForumRoles    A list of all forum roles.
	 */
	private static void handleDuplicateNamedRoles(List<ForumRole> memberForumRoles, Iterable<ForumRole> allForumRoles) {
		List<ForumRole> dupRoles = new ArrayList<>();
		for (ForumRole memberRole : memberForumRoles) {
			for (ForumRole forumRole : allForumRoles) {
				if (isDuplicateRoleName(memberForumRoles, memberRole, forumRole)) {
					dupRoles.add(forumRole);
				}
			}
		}

		memberForumRoles.addAll(dupRoles);
	}

	/**
	 * Checks if a member has a role that has the same name as one of his roles.
	 *
	 * @param memberForumRoles The roles of the member.
	 * @param memberRole       The role of the member to check.
	 * @param forumRole        The forum role to compare to.
	 * @return {@code true} if the {@param forumRole} is none of the members' roles, has the same name as one of the
	 * members' roles but another role ID.
	 */
	private static boolean isDuplicateRoleName(final List<ForumRole> memberForumRoles, final ForumRole memberRole, final ForumRole forumRole) {
		if (memberRole.getRoleName().equalsIgnoreCase(forumRole.getRoleName()) && memberRole.getRoleId() != forumRole.getRoleId()) {
			return !memberForumRoles.contains(forumRole);
		}

		return false;
	}

	/**
	 * Adds roles to a Discord member if he does not have it already.
	 *
	 * @param member The member to update the roles of.
	 * @param roles  The roles to add to the member.
	 */
	private static void addRoles(final Member member, final List<Role> roles) {
		Guild guild = member.getGuild();
		for (Role role : roles) {
			if (member.getRoles().contains(role)) {
				continue;
			}

			guild.addRoleToMember(member, role).queue(
					v -> LogUtil.logDebug(
							"Added role \"" + role.getName() + "\" to member \"" +
									member.getUser().getAsTag() + "\" (" + member.getId() + ")."
					),
					throwable -> LogUtil.logDebug(
							"Could not add role \"" + role.getName() + "\" to member \"" +
									member.getUser().getAsTag() + "\" (" + member.getId() + ")  due to \"" + throwable.getMessage() + "\"."
					)
			);
		}
	}

	/**
	 * Removes roles from a Discord member if he has it assigned.
	 *
	 * @param member The member to update the roles of.
	 * @param roles  The roles to remove from the member.
	 */
	private static void removeRoles(final Member member, final List<Role> roles) {
		Guild guild = member.getGuild();
		for (Role role : roles) {
			if (!member.getRoles().contains(role)) {
				continue;
			}

			guild.removeRoleFromMember(member, role).queue(
					v -> LogUtil.logDebug("Removed role \"" + role.getName() + "\" from member \"" +
							member.getUser().getAsTag() + "\" (" + member.getId() + ")."
					),
					throwable -> LogUtil.logDebug(
							"Could not remove role \"" + role.getName() + "\" from member \"" +
									member.getUser().getAsTag() + "\" (" + member.getId() + ")  due to \"" + throwable.getMessage() + "\"."
					)
			);
		}
	}

	/**
	 * Checks if the user has a role with the same ID as the banned role. Returns false if no role ID is
	 * set in the environment variables.
	 *
	 * @param envSettings The class that handles the environment variables.
	 * @param forumRoles  The list of forum roles the user has.
	 * @return {@code true} if the user has the banned role on the forum, {@code false} if there is no
	 * banned role ID set, the user does not have the banned role or if the ID exceeds Integer range.
	 */
	public static boolean hasBannedRole(final EnvSettings envSettings, final List<ForumRole> forumRoles) {
		final String bannedRoleIdStr = envSettings.getForumBannedRoleId();
		if (bannedRoleIdStr.isBlank()) {
			return false;
		}

		final long bannedRoleId = ParseUtil.safelyParseStringToLong(bannedRoleIdStr);
		if (bannedRoleId == -1) {
			LogUtil.logWarning("Invalid banned role ID! Please set a valid ID or remove the environment variable completely.");
			return false;
		}

		for (ForumRole forumRole : forumRoles) {
			if (forumRole.getRoleId() == bannedRoleId) {
				return true;
			}
		}

		return false;
	}
}
