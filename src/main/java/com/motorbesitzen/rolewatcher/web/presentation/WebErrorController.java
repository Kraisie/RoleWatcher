package com.motorbesitzen.rolewatcher.web.presentation;


import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Handles the error page content.
 */
@RestController
public class WebErrorController implements ErrorController {

	private final EnvSettings envSettings;

	@Autowired
	private WebErrorController(final EnvSettings envSettings) {
		this.envSettings = envSettings;
	}

	/**
	 * Redirects to an external error page set in the environment variables.
	 *
	 * @return The redirection.
	 */
	@RequestMapping(value = "/error", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.HEAD, RequestMethod.DELETE})
	private ResponseEntity<?> showErrorPage() {
		final String redirectUrl = envSettings.getErrorRedirectUrl();
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
	}

	@Deprecated
	@Override
	public String getErrorPath() {
		return null;
	}
}
