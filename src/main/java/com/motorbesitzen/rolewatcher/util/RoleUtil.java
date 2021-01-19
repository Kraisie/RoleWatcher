package com.motorbesitzen.rolewatcher.util;

import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
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
		List<Role> rolesToAdd = new ArrayList<>();
		List<Role> rolesToRemove = new ArrayList<>();
		handleDuplicateNamedRoles(memberForumRoles, allForumRoles);

		for (ForumRole forumRole : allForumRoles) {
			List<Role> matchingRoles = guild.getRolesByName(forumRole.getRoleName(), true);
			if (matchingRoles.size() == 0) {
				continue;
			}

			Role matchingRole = matchingRoles.get(0);
			if (memberForumRoles.contains(forumRole)) {
				rolesToAdd.add(matchingRole);
			} else {
				rolesToRemove.add(matchingRole);
			}
		}

		guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();
	}

	/**
	 * If there are two roles with the same name but different IDs then just add both to the users' roles.
	 * So the role does not get removed in Discord as{@code guild.modifyMemberRoles(...)} removes the role
	 * if it is present in the add and remove list.
	 *
	 * @param memberForumRoles The roles of the member.
	 * @param allForumRoles    A list of all forum roles.
	 */
	private static void handleDuplicateNamedRoles(List<ForumRole> memberForumRoles, Iterable<ForumRole> allForumRoles) {
		for (ForumRole forumRole : allForumRoles) {
			for (ForumRole dupForumRole : allForumRoles) {
				if (forumRole.getRoleName().equalsIgnoreCase(dupForumRole.getRoleName())) {
					if (!memberForumRoles.contains(forumRole)) {
						memberForumRoles.add(forumRole);
					}

					if (!memberForumRoles.contains(dupForumRole)) {
						memberForumRoles.add(dupForumRole);
					}
				}
			}
		}
	}

	/**
	 * Checks if the user has a role with the same ID as the banned role. Returns false if no role ID is
	 * set in the environment variables.
	 *
	 * @param forumRoles The list of forum roles the user has.
	 * @return {@code true} if the user has the banned role on the forum, {@code false} if there is no
	 * banned role ID set, the user does not have the banned role or if the ID exceeds Integer range.
	 */
	public static boolean hasBannedRole(List<ForumRole> forumRoles) {
		String bannedRoleIdStr = EnvironmentUtil.getEnvironmentVariableOrDefault("FORUM_BANNED_ROLE_ID", "");
		if (bannedRoleIdStr.isBlank()) {
			return false;
		}

		long bannedRoleId = ParseUtil.safelyParseStringToLong(bannedRoleIdStr);
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
