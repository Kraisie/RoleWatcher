package com.motorbesitzen.rolewatcher.web.presentation;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Handles the error page content.
 */
@Controller
public class WebErrorController implements ErrorController {

	/**
	 * Redirects to an external error page set in the environment variables.
	 *
	 * @return The redirection.
	 */
	@RequestMapping(value = "/error", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.HEAD, RequestMethod.DELETE})
	private String showErrorPage() {
		return "error.html";
	}

	@Deprecated
	@Override
	public String getErrorPath() {
		return null;
	}
}
