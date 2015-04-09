/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;

import java.util.Locale;

/**
 * An {@link AbstractValidator} which verifies that a {@link #date}
 * represented as a {@link String} can be parsed using a given {@link #locale}
 * and {@link #pattern}
 *
 * @since 3.7.0
 */
public class DateValidator extends AbstractValidator
{

    private final String date;
    private final String locale;
    private final String pattern;

    public DateValidator(String date, String locale, String pattern, ValidationContext validationContext)
    {
        super(validationContext);
        this.date = date;
        this.locale = locale;
        this.pattern = pattern;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        org.apache.commons.validator.routines.DateValidator validator = org.apache.commons.validator.routines.DateValidator.getInstance();
        Locale locale = new Locale(this.locale);

        boolean valid = pattern != null ? validator.isValid(date, pattern, locale) : validator.isValid(date, locale);
        return valid ? ok() : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidDate(date, locale, pattern);
    }
}
