/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.api.NestedProcessor;
import org.mule.extension.annotations.ImplementationOf;
import org.mule.extension.annotations.Operation;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.annotations.RestrictedTo;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.validation.ValidationExtension;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.validator.BooleanValidator;
import org.mule.extension.validation.validator.CreditCardNumberValidator;
import org.mule.extension.validation.validator.CreditCardType;
import org.mule.extension.validation.validator.DomainCountryCodeValidator;
import org.mule.extension.validation.validator.DomainValidator;
import org.mule.extension.validation.validator.DoubleValidator;
import org.mule.extension.validation.validator.EmailValidator;
import org.mule.extension.validation.validator.EmptyValidator;
import org.mule.extension.validation.validator.FloatValidator;
import org.mule.extension.validation.validator.ISBN10Validator;
import org.mule.extension.validation.validator.ISBN13Validator;
import org.mule.extension.validation.validator.IntegerValidator;
import org.mule.extension.validation.validator.IpValidator;
import org.mule.extension.validation.validator.LongValidator;
import org.mule.extension.validation.validator.MatchesRegexValidator;
import org.mule.extension.validation.validator.NotEmptyValidator;
import org.mule.extension.validation.validator.NotNullValidator;
import org.mule.extension.validation.validator.NullValidator;
import org.mule.extension.validation.validator.NumberValidationOptions;
import org.mule.extension.validation.validator.ShortValidator;
import org.mule.extension.validation.validator.SizeValidator;
import org.mule.extension.validation.validator.TimeValidator;
import org.mule.extension.validation.validator.TopLevelDomainValidator;
import org.mule.extension.validation.validator.UrlValidator;
import org.mule.transport.NullPayload;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Defines the operations of {@link ValidationExtension}
 * which executes the {@link Validator}s that the extension
 * provides out of the box
 *
 * @see ValidationExtension
 * @since 3.7.0
 */
@ImplementationOf(ValidationExtension.class)
public class CommonValidationOperations extends ValidationSupport
{

    public CommonValidationOperations(ValidationExtension config)
    {
        super(config);
    }

    /**
     * Validates that the given {@code value} is {@code true}
     *
     * @param expression the boolean to test
     * @param options    the {@link ValidationOptions}
     * @param event      the current {@link MuleEvent
     * @throws Exception if the value is not {@code true}
     */
    @Operation
    public void isTrue(boolean expression, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new BooleanValidator(expression, true, context), context, event);
    }

    /**
     * Validates that the given {@code value} is {@code false}
     *
     * @param expression the boolean to test
     * @param options    the {@link ValidationOptions}
     * @param event      the current {@link MuleEvent
     * @throws Exception if the value is not {@code true}
     */
    @Operation
    public void isFalse(boolean expression, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new BooleanValidator(expression, false, context), context, event);
    }

    /**
     * Fails if {@code creditCardNumber} is not a valid credit card number throw an exception.
     *
     * @param creditCardNumber the credit card number to validate
     * @param creditCardType   the card's type
     * @param options          the {@link ValidationOptions}
     * @param event            the current {@link MuleEvent
     */
    @Operation
    public void creditCardNumber(String creditCardNumber,
                                 CreditCardType creditCardType,
                                 @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new CreditCardNumberValidator(creditCardNumber, creditCardType, context), context, event);
    }

    /**
     * Validates that a {@code date} in {@link String} format is valid for the given {@code pattern} and {@code locale}.
     * If no pattern is provided, then the {@code locale}'s default will be used
     *
     * @param date    A date in String format
     * @param locale  the locale of the String
     * @param pattern the pattern for the {@code date}
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void date(String date,
                     @Optional(defaultValue = "US") String locale,
                     String pattern,
                     @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new org.mule.extension.validation.validator.DateValidator(date, locale, pattern, context), context, event);

    }

    /**
     * Validates that the specified {@code domain} is a valid one name with a
     * recognized top-level domain.
     *
     * @param domain  the domain name to validate
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void domain(String domain, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new DomainValidator(domain, context), context, event);
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and validates that it can be parsed as a {@link Double}
     *
     * @param numberValidationOptions the number options
     * @param options                 the {@link ValidationOptions}
     * @param event                   the current {@link MuleEvent
     */
    @Operation
    public void isDouble(@ParameterGroup NumberValidationOptions numberValidationOptions,
                         @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new DoubleValidator(numberValidationOptions, context), context, event);
    }

    /**
     * Validates that the {@code email} address is valid
     *
     * @param email   an email address
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void email(String email, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new EmailValidator(email, context), context, event);
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and validates that it can be parsed as a {@link Float}
     *
     * @param numberValidationOptions the number options
     * @param options                 the {@link ValidationOptions}
     * @param event                   the current {@link MuleEvent
     */
    @Operation
    public void isFloat(@ParameterGroup NumberValidationOptions numberValidationOptions,
                        @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new FloatValidator(numberValidationOptions, context), context, event);
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and validates that it can be parsed as a {@link Integer}
     *
     * @param numberValidationOptions the number options
     * @param options                 the {@link ValidationOptions}
     * @param event                   the current {@link MuleEvent
     */
    @Operation
    public void isInteger(@ParameterGroup NumberValidationOptions numberValidationOptions,
                          @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new IntegerValidator(numberValidationOptions, context), context, event);
    }

    /**
     * Validates that an {@code ip} address represented as a {@link String} is valid
     *
     * @param ip      the ip address to validate
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void ip(String ip, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new IpValidator(ip, context), context, event);
    }

    /**
     * Validates that the supplied {@code isbn} is a valid ISBN10 code
     *
     * @param isbn    the code to validate
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void isbn10(String isbn, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new ISBN10Validator(isbn, context), context, event);
    }

    /**
     * Validates that the supplied {@code isbn} is a valid ISBN13 code
     *
     * @param isbn    the code to validate
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void isbn13(String isbn, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new ISBN13Validator(isbn, context), context, event);
    }

    /**
     * Validates that {@code value} has a size between certain inclusive boundaries. This
     * validator is capable of handling instances of {@link String}, {@link Collection},
     * {@link Map} and arrays
     *
     * @param value   the value to validate
     * @param min     the minimum expected length (inclusive, defaults to zero)
     * @param max     the maximum expected length (inclusive). Leave unspecified or {@code null} to allow any max length
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void size(Object value,
                     @Optional(defaultValue = "0") int min,
                     @Optional Integer max,
                     @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new SizeValidator(value, min, max, context), context, event);
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and validates that it can be parsed as a {@link Long}
     *
     * @param numberValidationOptions the number options
     * @param options                 the {@link ValidationOptions}
     * @param event                   the current {@link MuleEvent
     */
    @Operation
    public void isLong(@ParameterGroup NumberValidationOptions numberValidationOptions,
                       @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new LongValidator(numberValidationOptions, context), context, event);
    }

    /**
     * Validates that {@code value} is not empty. The definition of empty depends on
     * the type of {@code value}. If it's a {@link String} it will check that it is not blank.
     * If it's a {@link Collection}, array or {@link Map} it will check that it's not empty. No other types
     * are supported, an {@link IllegalArgumentException} will be thrown if any other type is supplied
     *
     * @param value   the value to check
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     * @throws IllegalArgumentException if {@code value} is something other than a {@link String},{@link Collection} or {@link Map}
     */
    @Operation
    public void notEmpty(Object value, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new NotEmptyValidator(value, context), context, event);
    }

    /**
     * Validates that {@code value} is empty. The definition of empty depends on
     * the type of {@code value}. If it's a {@link String} it will check that it is not blank.
     * If it's a {@link Collection}, array or {@link Map} it will check that it's not empty. No other types
     * are supported, an {@link IllegalArgumentException} will be thrown if any other type is supplied
     *
     * @param value   the value to check
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent}
     * @param event   the current {@link MuleEvent
     * @throws IllegalArgumentException if {@code value} is something other than a {@link String},{@link Collection} or {@link Map}
     */
    @Operation
    public void isEmpty(Object value, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new EmptyValidator(value, context), context, event);
    }

    /**
     * Validates that the given {@code value} is not {@code null} nor
     * an instance of {@link NullPayload}
     *
     * @param value   the value to test
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void notNull(Object value, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new NotNullValidator(value, context), context, event);
    }

    /**
     * Validates that the given {@code value} is {@code null} or
     * an instance of {@link NullPayload}
     *
     * @param value   the value to test
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void isNull(Object value, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new NullValidator(value, context), context, event);
    }

    /**
     * Receives a numeric {@code value} as a {@link String} and validates that it can be parsed as a {@link Short}
     *
     * @param numberValidationOptions the number options
     * @param options                 the {@link ValidationOptions}
     * @param event                   the current {@link MuleEvent
     */
    @Operation
    public void isShort(@ParameterGroup NumberValidationOptions numberValidationOptions,
                        @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new ShortValidator(numberValidationOptions, context), context, event);
    }

    /**
     * Validates that a {@code time} in {@link String} format is valid for the given {@code pattern} and {@code locale}.
     * If no pattern is provided, then the {@code locale}'s default will be used
     *
     * @param time    A date in String format
     * @param locale  the locale of the String
     * @param pattern the pattern for the {@code date}
     * @param options the {@link ValidationOptions}
     * @param event   the current {@link MuleEvent
     */
    @Operation
    public void time(String time,
                     @Optional(defaultValue = "US") String locale,
                     @Optional String pattern,
                     @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new TimeValidator(time, locale, pattern, context), context, event);
    }

    /**
     * Validates that {@code countryCode} matches any IANA-defined
     * top-level domain country code. Leading dots are ignored if present. The
     * search is case-sensitive.
     *
     * @param countryCode the country code to validate
     * @param options     the {@link ValidationOptions}
     * @param event       the current {@link MuleEvent
     */
    @Operation
    public void topLevelDomainCountryCode(String countryCode, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new DomainCountryCodeValidator(countryCode, context), context, event);
    }

    /**
     * Validates that {@code domain} matches any IANA-defined
     * top-level domain. Leading dots are ignored if present. The
     * search is case-sensitive.
     *
     * @param topLevelDomain the domain to validate
     * @param options        the {@link ValidationOptions}
     * @param event          the current {@link MuleEvent
     */
    @Operation
    public void topLevelDomain(String topLevelDomain, @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new TopLevelDomainValidator(topLevelDomain, context), context, event);
    }

    /**
     * Validates that {@code url} is a valid one
     *
     * @param url             the URL to validate as a {@link String}
     * @param allowTwoSlashes Whether to allow two slashes in the path component of the URL
     * @param allowAllSchemes Whether to allow all validly formatted schemes to pass validation
     * @param allowLocalUrls  Whether to allow local URLs, such as http://localhost/
     * @param noFragments     Enabling this options disallows any URL fragment
     * @param options         the {@link ValidationOptions}
     * @param event           the current {@link MuleEvent
     */
    @Operation
    public void url(String url,
                    @Optional(defaultValue = "true") boolean allowTwoSlashes,
                    @Optional(defaultValue = "true") boolean allowAllSchemes,
                    @Optional(defaultValue = "true") boolean allowLocalUrls,
                    @Optional(defaultValue = "false") boolean noFragments,
                    @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new UrlValidator(url, allowTwoSlashes, allowAllSchemes, allowLocalUrls, noFragments, context), context, event);
    }

    /**
     * Validates that {@code value} matches the {@code regex} regular expression
     *
     * @param value         the value to check
     * @param regex         the regular expression to check against
     * @param caseSensitive when {@code true} matching is case sensitive, otherwise matching is case in-sensitive
     * @param options       the {@link ValidationOptions}
     * @param event         the current {@link MuleEvent
     */
    @Operation
    public void matchesRegex(String value,
                             String regex,
                             @Optional(defaultValue = "true") boolean caseSensitive,
                             @ParameterGroup ValidationOptions options, MuleEvent event) throws Exception
    {
        ValidationContext context = createContext(options, event);
        validateWith(new MatchesRegexValidator(value, regex, caseSensitive, context), context, event);
    }

    /**
     * Perform a list of nested validation operations and informs only one {@link ValidationResult}
     * which summarizes all of the found errors (if any).
     * <p/>
     * If {@code throwsException} is {@code true}, then the {@link ValidationResult}
     * is communicated by throwing a {@link ValidationException}. On the other hand,
     * if {@code throwsException} is {@code false}, then the {@link ValidationResult}
     * is set as the message payload.
     * <p/>
     * When configured through XML, all the {@code validations} must include
     * the All the child processors must contain the {@code validator-message-processor}
     * substitution group.
     *
     * @param validations     the nested validation operations
     * @param throwsException if {@code true}, then a {@link ValidationException} is thrown if any validator fails.
     * @param muleEvent       the current {@link MuleEvent}
     * @return the same {@code muleEvent} that was passed as argument
     * @throws ValidationException if at least one validator fails and {@code throwsException} is {@code true}
     */
    @Operation
    public MuleEvent all(@RestrictedTo(ValidationExtension.class) List<NestedProcessor> validations,
                         @Optional(defaultValue = "true") boolean throwsException,
                         MuleEvent muleEvent) throws ValidationException
    {
        List<String> messages = new ArrayList<>(validations.size());
        for (NestedProcessor validation : validations)
        {
            try
            {
                validation.process();
            }
            catch (Exception e)
            {
                messages.add(ExceptionUtils.getRootCause(e).getMessage());
            }
        }

        ValidationResult result = messages.isEmpty()
                                  ? ok()
                                  : error(Joiner.on('\n').join(messages));

        if (result.isError() && throwsException)
        {
            throw new ValidationException(result);
        }

        muleEvent.getMessage().setPayload(result);
        return muleEvent;
    }
}
