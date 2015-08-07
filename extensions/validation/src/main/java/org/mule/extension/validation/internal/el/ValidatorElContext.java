/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.el;

import static org.mule.extension.validation.internal.ValidationExtension.DEFAULT_LOCALE;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.internal.ValidationContext;
import org.mule.extension.validation.internal.ValidationExtension;
import org.mule.extension.validation.internal.ValidationMessages;
import org.mule.extension.validation.internal.ValidationOptions;
import org.mule.extension.validation.internal.validator.EmailValidator;
import org.mule.extension.validation.internal.validator.EmptyValidator;
import org.mule.extension.validation.internal.validator.IpValidator;
import org.mule.extension.validation.internal.validator.MatchesRegexValidator;
import org.mule.extension.validation.internal.validator.NotEmptyValidator;
import org.mule.extension.validation.internal.validator.NotNullValidator;
import org.mule.extension.validation.internal.validator.NullValidator;
import org.mule.extension.validation.internal.validator.NumberType;
import org.mule.extension.validation.internal.validator.NumberValidator;
import org.mule.extension.validation.internal.validator.SizeValidator;
import org.mule.extension.validation.internal.validator.TimeValidator;
import org.mule.extension.validation.internal.validator.UrlValidator;
import org.mule.transport.NullPayload;

import java.util.Locale;

/**
 * A class which allows executing instances of
 * {@link org.mule.extension.validation.internal.validator.AbstractValidator}'s
 * from a MEL context.
 * <p/>
 * Unlike regular validations which throw an exception upon failure,
 * the methods in this class will only return boolean values
 * to indicate if the validation was successful or not. Also, no message
 * is returned in either case.
 * <p/>
 * Since in this case we only care about the boolean result of the validation,
 * all validations will be executed with the same {@link ValidationContext}
 * <p/>
 * {@link Validator} instances are not reused. A new one is created each time
 *
 * @since 3.7.0
 */
public final class ValidatorElContext
{

    private final MuleEvent event;
    private final ValidationContext validationContext;

    public ValidatorElContext(MuleEvent event)
    {
        this.event = event;
        validationContext = new ValidationContext(new ValidationMessages(), new ValidationOptions(), event);
    }

    public ValidatorElContext()
    {
        this.event = VoidMuleEvent.getInstance();
        validationContext = new ValidationContext(new ValidationMessages(), new ValidationOptions(), event);
    }

    /**
     * Validates the given {@code email} using a {@link EmailValidator}
     *
     * @param email an email address
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateEmail(String email)
    {
        return validate(new EmailValidator(email, validationContext));
    }

    /**
     * Validates that {@code value} matches the given {@code regex} using a
     * {@link MatchesRegexValidator}
     *
     * @param value         the value to check
     * @param regex         the regular expression to check against
     * @param caseSensitive when {@code true} matching is case sensitive, otherwise matching is case in-sensitive
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean matchesRegex(String value, String regex, boolean caseSensitive)
    {
        return validate(new MatchesRegexValidator(value, regex, caseSensitive, validationContext));
    }

    /**
     * Validates that {@code time} represents a time according to the
     * given {@code pattern} an the default locale {@link ValidationExtension#DEFAULT_LOCALE)
     * using a {@link TimeValidator}
     *
     * @param time    A date in String format
     * @param pattern the pattern for the {@code date}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isTime(String time, String pattern)
    {
        return isTime(time, pattern, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code time} represents a time according to the
     * given {@code pattern} an the given locale {@code locale}
     * using a {@link TimeValidator
     * <p/>
     * {@code locale} is expected to be a valid key as defined in
     * {@link Locale}
     *
     * @param time    A date in String format
     * @param pattern the pattern for the {@code date}
     * @param locale  a {@link Locale} key
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isTime(String time, String pattern, String locale)
    {
        return validate(new TimeValidator(time, locale, pattern, validationContext));
    }

    /**
     * Validates that {@code value} is empty by using
     * a {@link EmptyValidator}
     *
     * @param value the value to check
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isEmpty(Object value)
    {
        return validate(new EmptyValidator(value, validationContext));
    }

    /**
     * Validates that {@code value} is not empty by using
     * a {@link NotEmptyValidator}
     *
     * @param value the value to check
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean notEmpty(Object value)
    {
        return validate(new NotEmptyValidator(value, validationContext));
    }

    /**
     * Validates that {@code value} has a size between certain inclusive boundaries by
     * using a {@link SizeValidator}
     *
     * @param value the value to validate
     * @param min   the minimum expected length (inclusive, defaults to zero)
     * @param max   the maximum expected length (inclusive). Leave unspecified or {@code null} to allow any max length
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateSize(Object value, int min, int max)
    {
        return validate(new SizeValidator(value, min, max, validationContext));
    }

    /**
     * Validates that {@code value} is not {@code null} or
     * {@link NullPayload} by using a {@link NotNullValidator}
     *
     * @param value the value to test
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isNotNull(Object value)
    {
        return validate(new NotNullValidator(value, validationContext));
    }

    /**
     * Validates that {@code value} is {@code null} or
     * {@link NullPayload} by using a {@link NullValidator}
     *
     * @param value the value to test
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isNull(Object value)
    {
        return validate(new NullValidator(value, validationContext));
    }

    /**
     * Validates that {@code value} can be parsed into a {@link Number},
     * by the rules of a {@link NumberValidator}.
     * <p/>
     * No boundaries are checked. Default system pattern and {@link Locale} are used
     *
     * @param value      the value to test
     * @param numberType the type of number to validate against
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isNumber(String value, NumberType numberType)
    {
        return isNumber(value, numberType, null, null);
    }

    /**
     * Validates that {@code value} can be parsed into a {@link Number},
     * by the rules of a {@link NumberValidator}.
     * <p/>
     * Default system pattern and {@link Locale} are used
     *
     * @param value      the value to test
     * @param numberType the type of number to validate against
     * @param minValue   if not {@code null}, the parsed number is checked to be greater or equal than this
     * @param minValue   if not {@code null}, the parsed number is checked to be lower or equal than this
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isNumber(String value, NumberType numberType, Number minValue, Number maxValue)
    {
        return isNumber(value, numberType, minValue, maxValue, null, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code value} can be parsed into a {@link Number},
     * by the rules of a {@link NumberValidator}.
     *
     * @param value      the value to test
     * @param numberType the type of number to validate against
     * @param minValue   if not {@code null}, the parsed number is checked to be greater or equal than this
     * @param minValue   if not {@code null}, the parsed number is checked to be lower or equal than this
     * @param pattern    the pattern to use when parsing the {@code value}
     * @param locale     the locale as a {@link String} to use when parsing the {@code value}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isNumber(String value, NumberType numberType, Number minValue, Number maxValue, String pattern, String locale)
    {
        return validate(new NumberValidator(value, new Locale(locale), pattern, minValue, maxValue, numberType, validationContext));
    }

    /**
     * Validates that an {@code ip} address represented as a {@link String} is valid
     * by using a {@link IpValidator}
     *
     * @param ip the ip address to validate
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateIp(String ip)
    {
        return validate(new IpValidator(ip, validationContext));
    }

    /**
     * Validates that {@code url} is a valid one
     * by using a {@link UrlValidator}.
     *
     * @param url the URL to validate as a {@link String}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateUrl(String url)
    {
        return validate(new UrlValidator(url, validationContext));
    }

    private boolean validate(Validator validator)
    {
        return !validator.validate(event).isError();
    }
}