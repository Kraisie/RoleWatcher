package com.motorbesitzen.rolewatcher.data.dao;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class LinkingInformation {

	@Id
	@Min(value = 1)
	private long uid;

	@NotNull
	@NotBlank
	@Length(max = 100)
	@JsonAlias("username")    // for jackson in web API
	private String forumUsername;

	@NotNull
	@NotBlank
	@Size(max = 20)
	@JsonAlias("verificationcode")    // for jackson in web API
	private String verificationCode;

	protected LinkingInformation() {
	}

	public LinkingInformation(long uid, String forumUsername, String verificationCode) {
		this.uid = uid;
		this.forumUsername = forumUsername;
		this.verificationCode = verificationCode;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public String getForumUsername() {
		return forumUsername;
	}

	public void setForumUsername(String forumUsername) {
		this.forumUsername = forumUsername;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public void setVerificationCode(String verificationCode) {
		this.verificationCode = verificationCode;
	}

	@Override
	public String toString() {
		return "{uid=" + uid + ", forumUsername='" + forumUsername + "', verificationCode='" + verificationCode + "'}";
	}
}
