/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.api.MuleEvent;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.util.StringUtils;

import java.util.Locale;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for validation operations with common
 * concerns
 *
 * @since 3.7.0
 */
abstract class ValidationSupport
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected void validateWith(Validator validator, ValidationContext validationContext, MuleEvent event) throws Exception
    {
        ValidationResult result = validator.validate(event);
        if (result.isError())
        {
            result = evaluateCustomMessage(result, validationContext);
            String customExceptionClass = validationContext.getOptions().getExceptionClass();
            if (StringUtils.isEmpty(customExceptionClass))
            {
                throw validationContext.getConfig().getExceptionFactory().createException(result, ValidationException.class, event);
            }
            else
            {
                throw validationContext.getConfig().getExceptionFactory().createException(result, customExceptionClass, event);
            }
        }
        else
        {
             logSuccessfulValidation(validator, event);
        }
    }

    private ValidationResult evaluateCustomMessage(ValidationResult result, ValidationContext validationContext)
    {
        String customMessage = validationContext.getOptions().getMessage();
        if (!StringUtils.isBlank(customMessage))
        {
            result = error(validationContext.getMuleEvent().getMuleContext().getExpressionManager().parse(customMessage, validationContext.getMuleEvent()));
        }

        return result;
    }

    protected ValidationContext createContext(ValidationOptions options, MuleEvent muleEvent, ValidationExtension config)
    {
        return new ValidationContext(options, muleEvent, config);
    }

    protected Locale parseLocale(String locale)
    {
        locale = StringUtils.isBlank(locale) ? ValidationExtension.DEFAULT_LOCALE : locale;
        return new Locale(locale);
    }

    protected void logSuccessfulValidation(Validator validator, MuleEvent event)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Successfully executed validator {}", ToStringBuilder.reflectionToString(validator));
        }
    }
}
