package com.motorbesitzen.rolewatcher.web.entity.validation;

import com.motorbesitzen.rolewatcher.web.entity.validation.impl.ApiKeyValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require a key validation.
 */
@Constraint(validatedBy = ApiKeyValidator.class)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidApiKey {
	/**
	 * @return The message if the key is invalid.
	 */
	String message() default "Access forbidden!";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
