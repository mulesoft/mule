/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationExtension;
import org.mule.extension.validation.api.ValidationOptions;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;

import java.util.Locale;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for validation operations with common concerns
 *
 * @since 3.7.0
 */
abstract class ValidationSupport {

  protected static final String ERROR_GROUP = "Error options";
  protected final static Logger LOGGER = LoggerFactory.getLogger(ValidationSupport.class);

  protected void validateWith(Validator validator, ValidationContext validationContext) throws Exception {
    ValidationResult result = validator.validate();
    if (result.isError()) {
      result = evaluateCustomMessage(result, validationContext);
      throw new ValidationException(result);
    } else {
      logSuccessfulValidation(validator);
    }
  }

  private ValidationResult evaluateCustomMessage(ValidationResult result, ValidationContext validationContext) {
    String customMessage = validationContext.getOptions().getMessage();
    return isBlank(customMessage)
        ? result
        : error(customMessage);
  }

  protected ValidationContext createContext(ValidationOptions options, ValidationExtension config) {
    return new ValidationContext(options, config);
  }

  protected Locale parseLocale(String locale) {
    locale = isBlank(locale) ? ValidationExtension.DEFAULT_LOCALE : locale;
    return new Locale(locale);
  }

  protected void logSuccessfulValidation(Validator validator) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Successfully executed validator {}", ToStringBuilder.reflectionToString(validator));
    }
  }
}
