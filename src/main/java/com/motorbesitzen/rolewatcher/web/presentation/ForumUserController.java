package com.motorbesitzen.rolewatcher.web.presentation;

import com.motorbesitzen.rolewatcher.data.dao.*;
import com.motorbesitzen.rolewatcher.data.repo.DiscordBanRepo;
import com.motorbesitzen.rolewatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.rolewatcher.data.repo.ForumUserRepo;
import com.motorbesitzen.rolewatcher.data.repo.LinkingInformationRepo;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Handles user related requests.
 */
@RestController
@Validated
public class ForumUserController {

	private final ForumUserRepo forumUserRepo;
	private final LinkingInformationRepo linkingRepo;
	private final DiscordBanRepo banRepo;
	private final DiscordGuildRepo guildRepo;
	private final JDA jda;

	@Autowired
	public ForumUserController(final ForumUserRepo forumUserRepo, final LinkingInformationRepo linkingRepo,
							   final DiscordBanRepo banRepo, final DiscordGuildRepo guildRepo, final JDA jda) {
		this.forumUserRepo = forumUserRepo;
		this.linkingRepo = linkingRepo;
		this.banRepo = banRepo;
		this.guildRepo = guildRepo;
		this.jda = jda;
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
			}

			return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
		}

		try {
			linkingRepo.save(linkingInformation);
		} catch (DataIntegrityViolationException e) {
			handleDataIntegrityViolationException(e, linkingInformation);
		}

		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
	}

	/**
	 * Handles a DataIntegrityViolationException that may occur if there are multiple simultaneous requests
	 * for a user that produce a race condition on inserting the user.
	 * Tries to update the user entry with the newest information available.
	 *
	 * @param e                  The exception.
	 * @param linkingInformation The linking information for the user.
	 */
	private void handleDataIntegrityViolationException(final DataIntegrityViolationException e, final LinkingInformation linkingInformation) {
		linkingRepo.findById(linkingInformation.getUid()).ifPresentOrElse(
				oldLinkingInformation -> {
					oldLinkingInformation.setVerificationCode(linkingInformation.getVerificationCode());
					linkingRepo.save(oldLinkingInformation);
				},
				() -> LogUtil.logError("Received DataIntegrityViolationException for unknown user " + linkingInformation, e)
		);
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

		kickUser(forumUser);
		forumUser.setLinkedDiscordUser(null);
		forumUserRepo.save(forumUser);    // unlinking from discord user, otherwise won't delete entry
		forumUserRepo.delete(forumUser);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
	}

	/**
	 * Kicks an unlinked user from the Discord.
	 *
	 * @param forumUser The user to kick.
	 */
	private void kickUser(final ForumUser forumUser) {
		final DiscordUser dcUser = forumUser.getLinkedDiscordUser();
		if (dcUser.isWhitelisted()) {
			return;
		}

		final Iterable<DiscordGuild> dcGuilds = guildRepo.findAll();
		for (DiscordGuild dcGuild : dcGuilds) {
			if (!dcGuild.hasRoleSyncPerm()) {
				continue;
			}

			final Guild guild = jda.getGuildById(dcGuild.getGuildId());
			if (guild != null) {
				final long forumId = forumUser.getForumId();
				final long discordId = dcUser.getDiscordId();
				guild.kick(
						String.valueOf(dcUser.getDiscordId()),
						"Kicked due to self-unlink (" + forumId + " <-> " + discordId + ")."
				).queue(
						v -> LogUtil.logDebug("Kicked member due to self-unlink."),
						throwable -> LogUtil.logDebug("Could not kick self-unlinked user!", throwable)
				);
			}
		}
	}

	/**
	 * Displays basic information about the requested user.
	 *
	 * @param key     The API key used.
	 * @param id      The forum or Discord ID of the user to request the data of.
	 * @param request Information about the HTTP request.
	 * @return A response with the status code representing the result of the request (200 = success
	 * 400 = No ID found, 404 = user not found, 500 = some internal error) and the information in the body on success.
	 */
	@RequestMapping(value = "/users/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> memberInfo(@ValidApiKey @RequestParam(value = "key", required = false) final String key,
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
		return ResponseEntity.status(HttpStatus.OK).body(
				"F_ID: " + forumUser.getForumId() + ", D_ID: " + dcUser.getDiscordId() + ", " +
						"BAN: " + (dcBans.size() > 0 ? dcBans.get(0).getReason() : "none")
		);
	}
}
