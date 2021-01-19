package com.motorbesitzen.rolewatcher.web.presentation;

import com.motorbesitzen.rolewatcher.data.dao.DiscordUser;
import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.repo.DiscordUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.web.entity.User;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

/**
 * Handles POST requests to add a link between a forum user and a Discord user. Can be used by a mechanic to link
 * forum and Discord account in the forum itself which sends the requests to this API.
 */
@RestController
@Validated
public class AddForumUserController {

	private final ForumUserRepo forumUserRepo;
	private final DiscordUserRepo discordUserRepo;

	@Autowired
	public AddForumUserController(ForumUserRepo forumUserRepo, DiscordUserRepo discordUserRepo) {
		this.forumUserRepo = forumUserRepo;
		this.discordUserRepo = discordUserRepo;
	}

	/**
	 * Validates the {@param key} and if it is valid adds the {@param user} to the database (if valid user object).
	 *
	 * @param key           The API key used.
	 * @param user          The user to add.
	 * @param bindingResult The results of binding the JSON in the body to the {@link User} object.
	 * @return A response with the status code representing the result of the addition (204 = success, 422 = Invalid
	 * entity, 409 = the user already exists, 500 = some internal error).
	 */
	@RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> addMember(@ValidApiKey @RequestParam(value = "key", required = false) String key, @Valid @RequestBody User user,
									   BindingResult bindingResult) {
		// key does not get used as it gets validated before any of this code below even starts, DO NOT REMOVE KEY
		if (bindingResult.hasErrors()) {
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Invalid/corrupted data!");
		}

		if (forumUserRepo.existsById(user.getUid())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Forum user already linked to another Discord account!");
		}

		Optional<DiscordUser> discordUserOpt = discordUserRepo.findById(user.getDiscordid());
		DiscordUser discordUser = discordUserOpt.orElseGet(() -> DiscordUser.createDiscordUser(user.getDiscordid()));
		if (discordUser.getLinkedForumUser() != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Discord user already linked to another forum account!");
		}

		ForumUser forumUser = ForumUser.withLinkedDiscordUser(user.getUid(), user.getUsername(), discordUser);
		discordUser.setLinkedForumUser(forumUser);
		discordUserRepo.save(discordUser);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
	}
}
