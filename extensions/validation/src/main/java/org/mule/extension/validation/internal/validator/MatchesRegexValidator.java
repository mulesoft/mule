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

import org.apache.commons.validator.routines.RegexValidator;

/**
 * An {@link AbstractValidator} which tests that
 * a {@link #value} matches a given {@link #regex}
 *
 * @since 3.7.0
 */
public class MatchesRegexValidator extends AbstractValidator
{

    /**
     * The value to tests
     */
    private final String value;

    /**
     * The regex to test again
     */
    private final String regex;

    /**
     * Whether the test should be case sensitive or not
     */
    private final boolean caseSensitive;


    public MatchesRegexValidator(String value, String regex, boolean caseSensitive, ValidationContext validationContext)
    {
        super(validationContext);
        this.value = value;
        this.regex = regex;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        RegexValidator validator = new RegexValidator(new String[] {regex}, caseSensitive);
        return validator.isValid(value)
               ? ok()
               : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().regexDoesNotMatch(value, regex);
    }
}
