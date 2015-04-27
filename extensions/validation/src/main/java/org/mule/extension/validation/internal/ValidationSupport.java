/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.api.MuleEvent;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.util.StringUtils;

import java.util.Locale;


/**
 * Base class for validation operations with common
 * concerns
 *
 * @since 3.7.0
 */
abstract class ValidationSupport
{

    protected final ValidationExtension config;
    private final ValidationMessages messages;

    public ValidationSupport(ValidationExtension config)
    {
        this.config = config;
        messages = config.getMessageFactory();
    }

    protected void validateWith(Validator validator, ValidationContext validationContext, MuleEvent event) throws Exception
    {
        ValidationResult result = validator.validate(event);
        if (result.isError())
        {
            String customExceptionClass = validationContext.getOptions().getExceptionClass();
            if (StringUtils.isEmpty(customExceptionClass))
            {
                throw config.getExceptionFactory().createException(result, ValidationException.class, event);
            }
            else
            {
                throw config.getExceptionFactory().createException(result, customExceptionClass, event);
            }
        }
    }

    protected ValidationContext createContext(ValidationOptions options, MuleEvent muleEvent)
    {
        return new ValidationContext(messages, options, muleEvent);
    }

    protected Locale parseLocale(String locale)
    {
        locale = StringUtils.isBlank(locale) ? ValidationExtension.DEFAULT_LOCALE : locale;
        return new Locale(locale);
    }
}
