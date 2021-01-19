package com.motorbesitzen.rolewatcher.web.entity.validation.impl;

import com.motorbesitzen.rolewatcher.util.EnvironmentUtil;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validates if the given key is the one set in the environment variables.
 */
public class ApiKeyValidator implements ConstraintValidator<ValidApiKey, String> {

	private String selfAddKey;

	/**
	 * Initializes the validator.
	 *
	 * @param validApiKey The annotation for a valid API key.
	 */
	@Override
	public void initialize(ValidApiKey validApiKey) {
		this.selfAddKey = EnvironmentUtil.getEnvironmentVariable("FORUM_USER_ADD_API_KEY");
	}

	/**
	 * Checks if a key is valid by comparing it to the set key.
	 *
	 * @param key              The key to check.
	 * @param validatorContext The validator context.
	 * @return {@code true} if the key is valid, {@code false} otherwise.
	 */
	@Override
	public boolean isValid(String key, ConstraintValidatorContext validatorContext) {
		return key.equals(selfAddKey);
	}
}
