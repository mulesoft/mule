/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.el;

import static org.mule.extension.validation.internal.ValidationExtension.DEFAULT_LOCALE;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleEvent;
import org.mule.extension.validation.internal.ValidationExtension;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.internal.ValidationContext;
import org.mule.extension.validation.internal.ValidationMessages;
import org.mule.extension.validation.internal.ValidationOptions;
import org.mule.extension.validation.internal.validator.CreditCardNumberValidator;
import org.mule.extension.validation.internal.validator.CreditCardType;
import org.mule.extension.validation.internal.validator.DateValidator;
import org.mule.extension.validation.internal.validator.DomainCountryCodeValidator;
import org.mule.extension.validation.internal.validator.DoubleValidator;
import org.mule.extension.validation.internal.validator.EmailValidator;
import org.mule.extension.validation.internal.validator.EmptyValidator;
import org.mule.extension.validation.internal.validator.FloatValidator;
import org.mule.extension.validation.internal.validator.ISBN10Validator;
import org.mule.extension.validation.internal.validator.ISBN13Validator;
import org.mule.extension.validation.internal.validator.IntegerValidator;
import org.mule.extension.validation.internal.validator.IpValidator;
import org.mule.extension.validation.internal.validator.LongValidator;
import org.mule.extension.validation.internal.validator.MatchesRegexValidator;
import org.mule.extension.validation.internal.validator.NotEmptyValidator;
import org.mule.extension.validation.internal.validator.NotNullValidator;
import org.mule.extension.validation.internal.validator.NullValidator;
import org.mule.extension.validation.internal.validator.NumberValidationOptions;
import org.mule.extension.validation.internal.validator.ShortValidator;
import org.mule.extension.validation.internal.validator.SizeValidator;
import org.mule.extension.validation.internal.validator.TimeValidator;
import org.mule.extension.validation.internal.validator.TopLevelDomainValidator;
import org.mule.extension.validation.internal.validator.UrlValidator;
import org.mule.transport.NullPayload;

import com.google.common.base.Enums;
import com.google.common.base.Optional;

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
     * Validates that {@code date} represents a valid date
     * according to the given {@code pattern} and an the
     * default locale {@link ValidationExtension#DEFAULT_LOCALE)
     * by using a {@link DateValidator}
     *
     * @param date    A date in String format
     * @param pattern the pattern for the {@code date}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isDate(String date, String pattern)
    {
        return isDate(date, pattern, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code date} represents a valid date
     * according to the given {@code pattern} and {@code locale}
     * by using a {@link DateValidator}.
     * <p/>
     * {@code locale} is expected to be a valid key as defined in
     * {@link Locale}
     *
     * @param date    A date in String format
     * @param locale  the locale of the String
     * @param pattern the pattern for the {@code date}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isDate(String date, String pattern, String locale)
    {
        return validate(new DateValidator(date, locale, pattern, validationContext));
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
     * Validates that {@code value} represents a valid {@link Long}
     * with default locale of {@link ValidationExtension#DEFAULT_LOCALE)
     * by using a {@link LongValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isLong(String value, Number minValue, Number maxValue)
    {
        return isLong(value, minValue, maxValue, null, DEFAULT_LOCALE);
    }


    /**
     * Validates that {@code value} represents a valid {@link Long}
     * by using a {@link LongValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @param pattern  the pattern to use when parsing {@code value}
     * @param locale   a valid key as defined in {@link Locale}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isLong(String value, Number minValue, Number maxValue, String pattern, String locale)
    {
        NumberValidationOptions options = new NumberValidationOptions(value, locale, pattern, minValue, maxValue);
        return validate(new LongValidator(options, validationContext));
    }

    /**
     * Validates that {@code value} represents a valid {@link Double}
     * with default locale of {@link ValidationExtension#DEFAULT_LOCALE)
     * by using a {@link DoubleValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isDouble(String value, Number minValue, Number maxValue)
    {
        return isDouble(value, minValue, maxValue, null, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code value} represents a valid {@link Double}
     * by using a {@link DoubleValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @param pattern  the pattern to use when parsing {@code value}
     * @param locale   a valid key as defined in {@link Locale}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isDouble(String value, Number minValue, Number maxValue, String pattern, String locale)
    {
        NumberValidationOptions options = new NumberValidationOptions(value, locale, pattern, minValue, maxValue);
        return validate(new DoubleValidator(options, validationContext));
    }

    /**
     * Validates that {@code value} represents a valid {@link Integer}
     * with default locale of {@link ValidationExtension#DEFAULT_LOCALE)
     * by using a {@link IntegerValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isInteger(String value, Number minValue, Number maxValue)
    {
        return isInteger(value, minValue, maxValue, null, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code value} represents a valid {@link Integer}
     * by using a {@link IntegerValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @param pattern  the pattern to use when parsing {@code value}
     * @param locale   a valid key as defined in {@link Locale}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isInteger(String value, Number minValue, Number maxValue, String pattern, String locale)
    {
        NumberValidationOptions options = new NumberValidationOptions(value, locale, pattern, minValue, maxValue);
        return validate(new IntegerValidator(options, validationContext));
    }

    /**
     * Validates that {@code value} represents a valid {@link Short}
     * with default locale of {@link ValidationExtension#DEFAULT_LOCALE)
     * by using a {@link ShortValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isShort(String value, Number minValue, Number maxValue)
    {
        return isShort(value, minValue, maxValue, null, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code value} represents a valid {@link Short}
     * by using a {@link ShortValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @param pattern  the pattern to use when parsing {@code value}
     * @param locale   a valid key as defined in {@link Locale}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isShort(String value, Number minValue, Number maxValue, String pattern, String locale)
    {
        NumberValidationOptions options = new NumberValidationOptions(value, locale, pattern, minValue, maxValue);
        return validate(new ShortValidator(options, validationContext));
    }

    /**
     * Validates that {@code value} represents a valid {@link Float}
     * with default locale of {@link ValidationExtension#DEFAULT_LOCALE)
     * by using a {@link FloatValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isFloat(String value, Number minValue, Number maxValue)
    {
        return isFloat(value, minValue, maxValue, null, DEFAULT_LOCALE);
    }

    /**
     * Validates that {@code value} represents a valid {@link Float}
     * by using a {@link FloatValidator}
     *
     * @param value    the value to test
     * @param minValue An inclusive minimum that {@code value} is expected to meet
     * @param maxValue An inclusive maximum that {@code value} is expected to meet
     * @param pattern  the pattern to use when parsing {@code value}
     * @param locale   a valid key as defined in {@link Locale}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean isFloat(String value, Number minValue, Number maxValue, String pattern, String locale)
    {
        NumberValidationOptions options = new NumberValidationOptions(value, locale, pattern, minValue, maxValue);
        return validate(new FloatValidator(options, validationContext));
    }

    /**
     * Validates that {@code creditCardNumber} is valid
     * for the given {@code creditCardType} by using a
     * {@link CreditCardNumberValidator}
     *
     * @param creditCardNumber the credit card number to validate
     * @param creditCardType   a valid key defined in {@link CreditCardType}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateCreditCardNumber(String creditCardNumber, String creditCardType)
    {
        Optional<CreditCardType> ccType = Enums.getIfPresent(CreditCardType.class, creditCardType);
        checkArgument(ccType.isPresent(), "unknown credit card type " + creditCardType);
        return validateCreditCardNumber(creditCardNumber, ccType.get());
    }

    /**
     * Validates that {@code creditCardNumber} is valid
     * for the given {@code creditCardType} by using a
     * {@link CreditCardNumberValidator}
     *
     * @param creditCardNumber the credit card number to validate
     * @param creditCardType   the card's type
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateCreditCardNumber(String creditCardNumber, CreditCardType creditCardType)
    {
        return validate(new CreditCardNumberValidator(creditCardNumber, creditCardType, validationContext));
    }

    /**
     * Validates that {@code countryCode} matches any IANA-defined
     * top-level domain country code by using a {@link DomainCountryCodeValidator}
     *
     * @param countryCode the country code to validate
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateTopLevelDomainCountryCode(String countryCode)
    {
        return validate(new DomainCountryCodeValidator(countryCode, validationContext));
    }

    /**
     * Validates that {@code domain} matches any IANA-defined
     * top-level domain by using a {@link TopLevelDomainValidator}
     *
     * @param topLevelDomain the domain to validate
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateTopLevelDomain(String topLevelDomain)
    {
        return validate(new TopLevelDomainValidator(topLevelDomain, validationContext));
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
     * Validates that the supplied {@code isbn} is a valid ISBN13 code
     * by using a {@link ISBN13Validator}
     *
     * @param isbn the code to validate
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateIsbn13(String isbn)
    {
        return validate(new ISBN13Validator(isbn, validationContext));
    }

    /**
     * Validates that the supplied {@code isbn} is a valid ISBN10 code
     * by using a {@link ISBN10Validator}
     *
     * @param isbn the code to validate
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateIsbn10(String isbn)
    {
        return validate(new ISBN10Validator(isbn, validationContext));
    }

    /**
     * Validates that {@code url} is a valid one
     * by using a {@link UrlValidator}. Double slashes,
     * all schemas and local urls and fragments are allowed.
     *
     * @param url the URL to validate as a {@link String}
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateUrl(String url)
    {
        return validateUrl(url, true, true, true, false);
    }

    /**
     * Validates that {@code url} is a valid one
     * by using a {@link UrlValidator}
     *
     * @param url             the URL to validate as a {@link String}
     * @param allowTwoSlashes Whether to allow two slashes in the path component of the URL
     * @param allowAllSchemes Whether to allow all validly formatted schemes to pass validation
     * @param allowLocalUrls  Whether to allow local URLs, such as http://localhost/
     * @param noFragments     Enabling this options disallows any URL fragment
     * @return {@code true} if the validation succeeded. {@code false} otherwise
     */
    public boolean validateUrl(String url,
                               boolean allowTwoSlashes,
                               boolean allowAllSchemes,
                               boolean allowLocalUrls,
                               boolean noFragments)
    {
        return validate(new UrlValidator(url, allowTwoSlashes, allowAllSchemes, allowLocalUrls, noFragments, validationContext));
    }

    private boolean validate(Validator validator)
    {
        return !validator.validate(event).isError();
    }
}