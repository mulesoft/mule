/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.validation.api.Validator;
import org.mule.mvel2.compiler.BlankLiteral;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A {@link MessageFactory} that provides the feedback messages
 * for the {@link Validator}s that are provided out of the box.
 * By default, it uses a default bundle with the default messages
 * in english language, but it is also possible to provide a custom
 * bundle and {@link java.util.Locale}
 *
 * @since 3.7.0
 */
public final class ValidationMessages extends MessageFactory
{

    private final String bundlePath;
    private final java.util.Locale locale;

    /**
     * Creates an instance which uses the default
     * english language bundle which ships with
     * the module
     */
    public ValidationMessages()
    {
        this.bundlePath = getBundlePath("validation");
        locale = java.util.Locale.getDefault();
    }

    /**
     * Creates an instance which points to the given
     * {@code bundlePath} and {@code locale}
     *
     * @param bundlePath the path to a bundle
     * @param locale     a locale key to be used to construct a {@link java.util.Locale}
     */
    public ValidationMessages(String bundlePath, String locale)
    {
        this.bundlePath = bundlePath;
        this.locale = StringUtils.isEmpty(locale) ? java.util.Locale.getDefault() : new java.util.Locale(locale);
    }

    @Override
    protected ResourceBundle getBundle(String bundlePath)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("Loading resource bundle: " + bundlePath + " for locale " + locale);
        }
        final ResourceBundle.Control control = getReloadControl();
        ResourceBundle bundle = control != null
                                ? ResourceBundle.getBundle(bundlePath, locale, getClassLoader(), control)
                                : ResourceBundle.getBundle(bundlePath, locale, getClassLoader());

        return bundle;
    }

    /**
     * Generates a {@link Message} for a boolean validation that failed
     *
     * @param value    the value that was obtained
     * @param expected the value that was expected
     * @return a {@link Message}
     */
    public Message failedBooleanValidation(boolean value, boolean expected)
    {
        return createMessage(bundlePath, 1, expected, value);
    }

    /**
     * Generates a {@link Message} for a number type validation that failed
     *
     * @param value      the value that was tested
     * @param numberType the type that the tested value was expected to have
     * @return a {@link Message}
     */
    public Message invalidNumberType(Object value, String numberType)
    {
        return createMessage(bundlePath, 2, value, numberType);
    }

    /**
     * Returns a message for a {@code value} that was
     * expected to be lower than {@code boundary} but isn't
     *
     * @param value    the value
     * @param boundary the boundary
     * @return a {@link Message}
     */
    public Message lowerThan(Object value, Object boundary)
    {
        return createMessage(bundlePath, 3, value, boundary);
    }

    /**
     * Returns a message for a {@code value} that was
     * expected to be greater than {@code boundary} but isn't
     *
     * @param value    the value
     * @param boundary the boundary
     * @return a {@link Message}
     */
    public Message greaterThan(Object value, Object boundary)
    {
        return createMessage(bundlePath, 4, value, boundary);
    }

    /**
     * Generates a {@link Message} for an email validation
     * that failed
     *
     * @param email the validated email address
     * @return a {@link Message}
     */
    public Message invalidEmail(String email)
    {
        return createMessage(bundlePath, 5, email);
    }

    /**
     * Generates a {@link Message} for an ip address
     * validation that failed
     *
     * @param ip the validated ip address
     * @return a {@link Message}
     */
    public Message invalidIp(String ip)
    {
        return createMessage(bundlePath, 6, ip);
    }

    /**
     * Generates a {@link Message} for a value that was expected to have
     * a minimum size but didn't
     *
     * @param value      the tested value
     * @param minSize    the minimum boundary used in the validation
     * @param actualSize the actual {@code value} size
     * @return a {@link Message}
     */
    public Message lowerThanMinSize(Object value, int minSize, int actualSize)
    {
        return createMessage(bundlePath, 7, value, minSize, actualSize);
    }

    /**
     * Generates a {@link Message} for a value that was expected to have
     * a maximum size but didn't
     *
     * @param value      the tested value
     * @param maxSize    the maximum boundary used in the validation
     * @param actualSize the actual {@code value} size
     * @return a {@link Message}
     */
    public Message greaterThanMaxSize(Object value, int maxSize, int actualSize)
    {
        return createMessage(bundlePath, 8, value, maxSize, actualSize);
    }

    /**
     * Generates a {@link Message} for a value that is {@code null}
     * what it wasn't expected to be
     *
     * @return a {@link Message}
     */
    public Message valueIsNull()
    {
        return createMessage(bundlePath, 9);
    }

    /**
     * Generates a {@link Message} for a value that was
     * expected to be not empty but was
     *
     * @return a {@link Message}
     */
    public Message collectionIsEmpty()
    {
        return createMessage(bundlePath, 10);
    }

    /**
     * Generates a {@link Message} for a string
     * that was expected to be not blank
     * but wasn't
     *
     * @return a {@link Message}
     */
    public Message stringIsBlank()
    {
        return createMessage(bundlePath, 11);
    }

    /**
     * Generates a {@link Message} for a map
     * that was expected to be not empty
     * but was
     *
     * @return a {@link Message}
     */
    public Message mapIsEmpty()
    {
        return createMessage(bundlePath, 12);
    }

    /**
     * Generates a {@link Message} for a value that was expected
     * to not be a {@link BlankLiteral} but was
     *
     * @return a {@link Message}
     */
    public Message valueIsBlankLiteral()
    {
        return createMessage(bundlePath, 13);
    }

    /**
     * Generates a {@link Message} for a value that
     * was expected to be {@code null} but wasn't
     *
     * @return a {@link Message}
     */
    public Message wasExpectingNull()
    {
        return createMessage(bundlePath, 14);
    }

    /**
     * Generates a {@link Message} for a {@code time} expressed as a
     * {@link String} but couldn't be parsed using the given
     * {@code locale} and {@code pattern}
     *
     * @param time    the {@link String} that couldn't be parsed
     * @param locale  the locale that was used when parsing
     * @param pattern the pattern that was used when parsing
     * @return a {@link Message}
     */
    public Message invalidTime(String time, String locale, String pattern)
    {
        return createMessage(bundlePath, 15, time, pattern, locale);
    }

    /**
     * Generates a {@link Message} for a {@code url} that is not valid
     *
     * @param url the tested url
     * @return a {@link Message}
     */
    public Message invalidUrl(String url)
    {
        return createMessage(bundlePath, 16, url);
    }

    /**
     * Generates a {@link Message} for a {@code value} which doesn't
     * match a given {@code regex}
     *
     * @param value the tested value
     * @param regex the regex that the {@code value} was tested against
     * @return a {@link Message}
     */
    public Message regexDoesNotMatch(String value, String regex)
    {
        return createMessage(bundlePath, 17, value, regex);
    }

    /**
     * Generates a {@link Message} for an array which was expected
     * to be not empty but was
     *
     * @return a {@link Message}
     */
    public Message arrayIsEmpty()
    {
        return createMessage(bundlePath, 18);
    }

    /**
     * Generates a {@link Message} for a String which was expected
     * to be blank but wasn't
     *
     * @return a {@link Message}
     */
    public Message stringIsNotBlank()
    {
        return createMessage(bundlePath, 19);
    }

    /**
     * Generates a {@link Message} for a {@link Collection}
     * which was expected to be empty but wasn't
     *
     * @return a {@link Message}
     */
    public Message collectionIsNotEmpty()
    {
        return createMessage(bundlePath, 20);
    }

    /**
     * Generates a {@link Message} for a {@link Map}
     * which was expected to be empty but wasn't
     *
     * @return a {@link Message}
     */
    public Message mapIsNotEmpty()
    {
        return createMessage(bundlePath, 21);
    }

    /**
     * Generates a {@link Message} for an array
     * which was expected to be empty but wasn't
     *
     * @return a {@link Message}
     */
    public Message arrayIsNotEmpty()
    {
        return createMessage(bundlePath, 22);
    }
}
