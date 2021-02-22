package com.motorbesitzen.rolewatcher.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motorbesitzen.rolewatcher.data.dao.ForumRole;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.ForumRoleRepo;
import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class to perform forum API requests.
 */
@Service
public class ForumRoleApiRequest {

	private final ForumRoleRepo forumRoleRepo;

	@Autowired
	public ForumRoleApiRequest(ForumRoleRepo forumRoleRepo) {
		this.forumRoleRepo = forumRoleRepo;
	}

	/**
	 * Gets the forum roles for a specific user.
	 *
	 * @param forumUser The forum user to get the roles of.
	 * @return The list of forum roles the user has.
	 * @throws IOException If the roles can not be requested.
	 */
	public List<ForumRole> getRolesOfForumUser(final ForumUser forumUser) throws IOException {
		final String roleApi = EnvironmentUtil.getEnvironmentVariableOrDefault("FORUM_ROLE_API_URL", "");
		if (roleApi.isBlank()) {
			throw new IllegalStateException("Forum role API URL not set. Not able to receive forum roles!");
		}

		final long uid = forumUser.getForumId();
		final String roleApiUrl = roleApi + uid;
		final String roleIdsJson = getRoleIdsJson(roleApiUrl);
		return convertJsonToForumRoles(roleIdsJson);
	}

	/**
	 * Gets the raw JSON of the forum role API for a user.
	 *
	 * @param userApiUrl The URL to request the users forum roles from.
	 * @return The raw JSON representation of the roles.
	 * @throws IOException if the roles can not be requested.
	 */
	private String getRoleIdsJson(String userApiUrl) throws IOException {
		int timeoutMs = 10000;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeoutMs)
				.setConnectionRequestTimeout(timeoutMs)
				.setSocketTimeout(timeoutMs)
				.build();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		HttpGet request = new HttpGet(userApiUrl);
		HttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();
		BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
		String line = reader.readLine();
		reader.close();
		return line;
	}

	/**
	 * Converts the JSON of the forum role API to {@link ForumRole} objects.
	 *
	 * @param roleIdsJson The raw JSON representation of the roles.
	 * @return A list of {@link ForumRole}s the user has.
	 * @throws IllegalArgumentException if the JSON is invalid.
	 */
	private List<ForumRole> convertJsonToForumRoles(final String roleIdsJson) throws IllegalArgumentException {
		if (roleIdsJson == null) {
			throw new IllegalArgumentException("Could not convert role ID JSON to integer array! JSON is null.");
		}

		try {
			ObjectMapper mapper = new ObjectMapper();
			long[] roleIds = mapper.readValue(roleIdsJson, long[].class);
			return convertRoleIdsToForumRoles(roleIds);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not convert role ID JSON to integer array! JSON: \"" + roleIdsJson + "\"", e);
		}
	}

	/**
	 * Finds the fitting {@link ForumRole}s for the given IDs.
	 *
	 * @param roleIds The {@code int[]} of role IDs.
	 * @return A list of {@link ForumRole}s.
	 */
	private List<ForumRole> convertRoleIdsToForumRoles(long[] roleIds) {
		List<ForumRole> matchingRoles = new ArrayList<>();
		for (long roleId : roleIds) {
			Optional<ForumRole> roleOpt = forumRoleRepo.findById(roleId);
			if (roleOpt.isEmpty()) {
				continue;
			}

			matchingRoles.add(roleOpt.get());
		}

		return matchingRoles;
	}
}
