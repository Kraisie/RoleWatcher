package com.motorbesitzen.rolewatcher.data.dao;

import org.hibernate.validator.constraints.Length;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class ForumRole {

	@Id
	@Min(value = 1)
	private long roleId;

	@NotNull
	@NotBlank
	@Length(max = 100)
	private String roleName;

	protected ForumRole() {
	}

	private ForumRole(long roleId, String roleName) {
		this.roleId = roleId;
		this.roleName = roleName;
	}

	public static ForumRole of(long roleId, String roleName) {
		return new ForumRole(roleId, roleName);
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String name) {
		this.roleName = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ForumRole forumRole = (ForumRole) o;

		if (roleId != forumRole.roleId) return false;
		return roleName.equals(forumRole.roleName);
	}

	@Override
	public int hashCode() {
		int result = (int) (roleId ^ (roleId >>> 32));
		result = 31 * result + roleName.hashCode();
		return result;
	}
}
