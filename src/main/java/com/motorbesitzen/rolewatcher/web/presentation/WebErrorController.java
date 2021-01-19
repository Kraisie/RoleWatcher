package com.motorbesitzen.rolewatcher.web.presentation;


import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles the error page content.
 */
@RestController
public class WebErrorController implements ErrorController {

	/**
	 * Redirects to an internal or external error page set in the environment variables.
	 *
	 * @return The redirection.
	 */
	@RequestMapping(value = "/error", method = RequestMethod.GET)
	private ModelAndView showErrorPage() {
		String redirectUrl = EnvironmentUtil.getEnvironmentVariableOrDefault("ERROR_REDIRECT_URL", "https://theannoyingsite.com/");
		return new ModelAndView("redirect:" + redirectUrl);
	}

	// Deprecated Spring function, do not @Override
	public String getErrorPath() {
		return null;
	}
}
