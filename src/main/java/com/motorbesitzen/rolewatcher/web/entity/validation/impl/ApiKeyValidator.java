package com.motorbesitzen.rolewatcher.web.entity.validation.impl;

import com.motorbesitzen.rolewatcher.bot.service.EnvSettings;
import com.motorbesitzen.rolewatcher.web.entity.validation.ValidApiKey;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validates if the given key is the one set in the environment variables.
 */
public class ApiKeyValidator implements ConstraintValidator<ValidApiKey, String> {

	private final EnvSettings envSettings;
	private String selfAddKey;

	@Autowired
	private ApiKeyValidator(final EnvSettings envSettings) {
		this.envSettings = envSettings;
	}

	/**
	 * Initializes the validator.
	 *
	 * @param validApiKey The annotation for a valid API key.
	 */
	@Override
	public void initialize(ValidApiKey validApiKey) {
		this.selfAddKey = envSettings.getSelfaddApiKey();
	}

	/**
	 * Checks if a key is valid by comparing it to the set key.
	 *
	 * @param key              The key to check.
	 * @param validatorContext The validator context.
	 * @return {@code true} if the key is valid, {@code false} otherwise.
	 */
	@Override
	public boolean isValid(final String key, final ConstraintValidatorContext validatorContext) {
		if (key == null) {
			return false;
		}

		return key.equals(selfAddKey);
	}
}
