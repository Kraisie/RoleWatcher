package com.motorbesitzen.rolewatcher.web.presentation;

import com.motorbesitzen.rolewatcher.data.dao.ForumUser;
import com.motorbesitzen.rolewatcher.data.dao.LinkingInformation;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.LinkingInformationRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolationException;
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
	private final LinkingInformationRepo linkingRepo;

	@Autowired
	public AddForumUserController(final ForumUserRepo forumUserRepo, final LinkingInformationRepo linkingRepo) {
		this.forumUserRepo = forumUserRepo;
		this.linkingRepo = linkingRepo;
	}

	/**
	 * Validates the {@param key} and if it is valid adds the {@param linkingInformation} to the database if valid.
	 *
	 * @param key                The API key used.
	 * @param linkingInformation The information about the user to link.
	 * @param bindingResult      The results of binding the JSON in the body to the {@link LinkingInformation} object.
	 * @return A response with the status code representing the result of the addition (204 = success, 422 = Invalid
	 * entity, 409 = the user already exists, 400 = duplicate code, 500 = some internal error).
	 */
	@RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> addMember(@ValidApiKey @RequestParam(value = "key", required = false) final String key,
									   @Valid @RequestBody final LinkingInformation linkingInformation,
									   final BindingResult bindingResult) {
		// key does not get used as it gets validated before any of this code below even starts, DO NOT REMOVE KEY
		if (bindingResult.hasErrors()) {
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Invalid/corrupted data!");
		}

		final Optional<ForumUser> forumUserOpt = forumUserRepo.findById(linkingInformation.getUid());
		if (forumUserOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(
					"Your forum account is already linked! Contact a staff if you want to get unlinked."
			);
		}

		final Optional<LinkingInformation> savedInfoIdOpt = linkingRepo.findById(linkingInformation.getUid());
		final Optional<LinkingInformation> savedInfoCodeOpt = linkingRepo.findByVerificationCode(linkingInformation.getVerificationCode());
		if (savedInfoIdOpt.isPresent() && savedInfoCodeOpt.isPresent()) {
			final LinkingInformation savedInfoId = savedInfoIdOpt.get();
			final LinkingInformation savedInfoCode = savedInfoCodeOpt.get();
			final long savedInfoIdUid = savedInfoId.getUid();
			final long savedInfoCodeUid = savedInfoCode.getUid();
			if (savedInfoIdUid != savedInfoCodeUid) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}
		}

		if (savedInfoIdOpt.isPresent()) {
			final LinkingInformation savedInfo = savedInfoIdOpt.get();
			if (!savedInfo.getVerificationCode().equals(linkingInformation.getVerificationCode())) {
				savedInfo.setVerificationCode(linkingInformation.getVerificationCode());
				linkingRepo.save(savedInfo);
				return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
			}
		}

		linkingRepo.save(linkingInformation);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
	}

	/**
	 * Handles exception when JSON is valid but does not fulfill the constraints of the object.
	 *
	 * @param e The thrown exception.
	 * @return A response with the status code representing invalid data.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> handleConstraintViolation(final Exception e) {
		LogUtil.logError("Received constraint violation on mapping!", e);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Invalid/corrupted data!");
	}
}
