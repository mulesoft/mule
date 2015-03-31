/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;

import java.util.Locale;

/**
 * An {@link AbstractValidator} which verifies that a {@link #time}
 * represented as a {@link String} can be parsed using a given {@link #locale}
 * and {@link #pattern}
 *
 * @since 3.7.0
 */
public class TimeValidator extends AbstractValidator
{

    private final String time;
    private final String locale;
    private final String pattern;
    private Message errorMessage;

    public TimeValidator(String time, String locale, String pattern, ValidationContext validationContext)
    {
        super(validationContext);
        this.time = time;
        this.locale = locale;
        this.pattern = pattern;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        org.apache.commons.validator.routines.TimeValidator validator = org.apache.commons.validator.routines.TimeValidator.getInstance();
        Locale locale = new Locale(this.locale);
        if (pattern != null)
        {
            if (!validator.isValid(time, pattern, locale))
            {
                errorMessage = getMessages().invalidTime(time, this.locale, pattern);
                return fail();
            }
        }
        else
        {
            if (!validator.isValid(time, locale))
            {
                errorMessage = getMessages().invalidTime(time, this.locale, pattern);
                return fail();
            }
        }

        return ok();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return errorMessage;
    }
}
