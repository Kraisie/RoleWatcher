package com.motorbesitzen.rolewatcher.util;

import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleUtilTest {

	private final ForumRole admin = ForumRole.of(1, "Administrator");
	private final ForumRole customer = ForumRole.of(4, "Customer");
	private final ForumRole user = ForumRole.of(8, "User");
	private final ForumRole banned = ForumRole.of(666, "Banned");

	@Test
	@DisplayName("should return false when no banned role ID is set in the environment")
	void testFalseForNoBannedRoleSet() {
		List<ForumRole> userRoles = new ArrayList<>();
		userRoles.add(customer);
		userRoles.add(user);

		boolean result = RoleUtil.hasBannedRole(userRoles);

		assertThat(result).isFalse();
	}
}
