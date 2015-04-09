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

import org.springframework.util.StringUtils;

/**
 * Base class for validators that test that a String can be parsed into a numeric value
 * and is compliant with settings defined in a {@link NumberValidationOptions} object
 *
 * @since 3.7.0
 */
abstract class NumberValidator extends AbstractValidator
{

    private final NumberValidationOptions options;
    private Message errorMessage;

    public NumberValidator(NumberValidationOptions options, ValidationContext validationContext)
    {
        super(validationContext);
        this.options = options;
    }

    /**
     * Implement this method to perform the validation using a specific {@code pattern}
     *
     * @param value   the value to be tested
     * @param pattern the pattern to test against
     * @param locale  a {@link Locale} key to use when parsing
     * @return the parsed {@link Number\}
     */
    protected abstract Number validateWithPattern(String value, String pattern, Locale locale);

    /**
     * Implement this method to perform the validation without using a specific {@code pattern}
     *
     * @param value  the value to be tested
     * @param locale a {@link Locale} key to use when parsing
     * @return the parsed {@link Number\}
     */
    protected abstract Number validateWithoutPattern(String value, Locale locale);

    /**
     * The {@link Class} of the {@link Number} type this validator tests
     *
     * @return
     */
    protected abstract Class<? extends Number> getNumberType();

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        Comparable<Number> newValue;

        if (StringUtils.isEmpty(options.getPattern()))
        {
            newValue = (Comparable<Number>) validateWithoutPattern(options.getValue(), new Locale(options.getLocale()));
        }
        else
        {
            newValue = (Comparable<Number>) validateWithPattern(options.getValue(), options.getPattern(), new Locale(options.getLocale()));
        }

        if (newValue == null)
        {
            errorMessage = getMessages().invalidNumberType(options.getValue(), getNumberType());
            return fail();
        }

        if (options.getMinValue() != null)
        {
            if (newValue.compareTo(options.getMinValue()) < 0)
            {
                errorMessage = getMessages().lowerThan(newValue, options.getMinValue());
                return fail();
            }
        }

        if (options.getMaxValue() != null)
        {
            if (newValue.compareTo(options.getMaxValue()) > 0)
            {
                errorMessage = getMessages().greaterThan(newValue, options.getMaxValue());
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
