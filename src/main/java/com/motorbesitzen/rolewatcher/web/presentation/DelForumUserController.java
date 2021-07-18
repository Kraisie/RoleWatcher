package com.motorbesitzen.rolewatcher.web.presentation;

import com.motorbesitzen.rolewatcher.data.dao.DiscordBan;
import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordBanRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * Handles POST requests to delete a link between a forum user and a Discord user. Can be used by a mechanic to unlink
 * forum and Discord account in the forum itself which sends the requests to this API.
 */
@RestController
@Validated
public class DelForumUserController {

	private final ForumUserRepo forumUserRepo;
	private final DiscordBanRepo banRepo;

	@Autowired
	public DelForumUserController(final ForumUserRepo forumUserRepo, final DiscordBanRepo banRepo) {
		this.forumUserRepo = forumUserRepo;
		this.banRepo = banRepo;
	}

	/**
	 * Validates the {@param key} and if it is valid deletes the {@param linkingInformation} to the database if valid.
	 *
	 * @param key The API key used.
	 * @param id  The forum or Discord ID of the user to unlink.
	 * @return A response with the status code representing the result of the addition (204 = success, 422 = Invalid
	 * entity, 409 = the user already exists, 400 = duplicate code, 500 = some internal error).
	 */
	@RequestMapping(value = "/users/{id}", method = RequestMethod.DELETE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> delMember(@ValidApiKey @RequestParam(value = "key", required = false) final String key,
									   @PathVariable("id") final Long id, final HttpServletRequest request) {
		if (id == null) {
			LogUtil.logDebug(request.getRequestURI());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing ID!");
		}

		final Optional<ForumUser> forumUserOpt = forumUserRepo.findByForumIdOrLinkedDiscordUser_DiscordId(id, id);
		if (forumUserOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					"No user found with that ID!"
			);
		}

		final ForumUser forumUser = forumUserOpt.get();
		final DiscordUser dcUser = forumUser.getLinkedDiscordUser();
		final List<DiscordBan> dcBans = banRepo.findAllByBannedUser_DiscordId(dcUser.getDiscordId());
		if (dcBans.size() > 0) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(
					"Can not unlink banned user!"
			);
		}

		forumUser.setLinkedDiscordUser(null);
		forumUserRepo.save(forumUser);    // unlinking from discord user, otherwise won't delete entry
		forumUserRepo.delete(forumUser);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
	}
}
