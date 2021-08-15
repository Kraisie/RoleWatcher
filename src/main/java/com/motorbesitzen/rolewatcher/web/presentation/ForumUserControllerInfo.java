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

@RestController
@Validated
public class ForumUserControllerInfo {

	private final ForumUserRepo forumUserRepo;
	private final DiscordBanRepo banRepo;

	@Autowired
	public ForumUserControllerInfo(final ForumUserRepo forumUserRepo, final DiscordBanRepo banRepo) {
		this.forumUserRepo = forumUserRepo;
		this.banRepo = banRepo;
	}

	@RequestMapping(value = "/users/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> addMember(@ValidApiKey @RequestParam(value = "key", required = false) final String key,
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
