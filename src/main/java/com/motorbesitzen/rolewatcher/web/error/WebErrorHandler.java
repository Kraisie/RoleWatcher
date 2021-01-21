package com.motorbesitzen.rolewatcher.web.error;

import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import com.motorbesitzen.rolewatcher.util.LogUtil;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Handles errors of the web part.
 */
@ControllerAdvice(basePackages = "com.motorbesitzen.rolewatcher.web.presentation")
public class WebErrorHandler {

	/**
	 * Handles all exceptions that are not handled otherwise and logs them.
	 *
	 * @param e The exception.
	 * @return A response with the status code 500.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleInternalServerError(Exception e) {
		LogUtil.logError("Internal server error (5XX)!", e);
		return ResponseEntity.
				status(HttpStatus.INTERNAL_SERVER_ERROR).
				body("Internal server error, please inform the forum or Discord staff about this message!");
	}

	/**
	 * Handles the exception if a route does not support the used request method.
	 *
	 * @return A response with the status code 404.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<?> handleRequestMethodNotSupported(HttpServletRequest request) {
		LogUtil.logDebug("Request method \"" + request.getMethod() + "\" not supported for query: " + request.getQueryString());
		return ResponseEntity.
				status(HttpStatus.NOT_FOUND).
				body("Not found.");
	}

	/**
	 * Handles the exception if Jackson can not translate the body to an object.
	 *
	 * @param e       The exception.
	 * @param request Information about the request.
	 * @return A response with the status code 403 if the key is not correct or with the status code of 422 if the key is correct.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
		if (!request.getQueryString().equals("key=" + EnvironmentUtil.getEnvironmentVariable("FORUM_USER_ADD_API_KEY"))) {
			logInvalidApiKey(request);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access forbidden!");
		}

		LogUtil.logError("Invalid/corrupted JSON data received by self add API! ", e);
		return ResponseEntity.
				status(HttpStatus.UNPROCESSABLE_ENTITY).
				body("Invalid/corrupted data!");
	}

	/**
	 * Handles the exception if any constraint of an object is not met.
	 *
	 * @param e       The exception.
	 * @param request Information about the request.
	 * @return A response with the status code 403 if the key is not correct or with the status code of 400 if the key is correct.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
		Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
		for (ConstraintViolation<?> constraintViolation : constraintViolations) {
			Class<? extends Annotation> violatedAnnotation = constraintViolation.getConstraintDescriptor().getAnnotation().annotationType();
			if (ValidApiKey.class.equals(violatedAnnotation)) {
				logInvalidApiKey(request);
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access forbidden!");
			}
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad request.");
	}

	/**
	 * Logs requests to API paths with an invalid API key.
	 *
	 * @param request Information about the request.
	 */
	private void logInvalidApiKey(HttpServletRequest request) {
		LogUtil.logWarning(
				"SelfAdd API got accessed with the wrong key! " +
						"IP (wrong if proxies like nginx are used): " + request.getRemoteAddr() +
						", X-Forwarded-For IPs: " + request.getHeader("X-FORWARDED-FOR") +
						", query: \"" + request.getQueryString() + "\""
		);
	}
}
