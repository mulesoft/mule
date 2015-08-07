/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import java.util.Locale;

import org.apache.commons.validator.routines.DoubleValidator;
import org.apache.commons.validator.routines.FloatValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.LongValidator;
import org.apache.commons.validator.routines.ShortValidator;

/**
 * Defines types of {@link Number}s against which validations can be
 * performed
 *
 * @since 3.7.0
 */
public enum NumberType
{
    INTEGER
            {
                @Override
                public Number toNumber(String value, String pattern, Locale locale)
                {
                    return pattern != null
                           ? IntegerValidator.getInstance().validate(value, pattern, locale)
                           : IntegerValidator.getInstance().validate(value, locale);
                }
            },
    LONG
            {
                @Override
                public Number toNumber(String value, String pattern, Locale locale)
                {
                    return pattern != null
                           ? LongValidator.getInstance().validate(value, pattern, locale)
                           : LongValidator.getInstance().validate(value, locale);
                }
            },
    SHORT
            {
                @Override
                public Number toNumber(String value, String pattern, Locale locale)
                {
                    return pattern != null
                           ? ShortValidator.getInstance().validate(value, pattern, locale)
                           : ShortValidator.getInstance().validate(value, locale);
                }
            },
    DOUBLE
            {
                @Override
                public Number toNumber(String value, String pattern, Locale locale)
                {
                    return pattern != null
                           ? DoubleValidator.getInstance().validate(value, pattern, locale)
                           : DoubleValidator.getInstance().validate(value, locale);
                }
            },
    FLOAT
            {
                @Override
                public Number toNumber(String value, String pattern, Locale locale)
                {
                    return pattern != null
                           ? FloatValidator.getInstance().validate(value, pattern, locale)
                           : FloatValidator.getInstance().validate(value, locale);
                }
            };

    /**
     * Parses the given {@code value} using a {@code pattern} and
     * {@code locale} into a {@link Number} instance
     *
     * @param value   the {@link String} to parse
     * @param pattern the pattern to parse against. If {@code null} then a system default is used
     * @param locale  the {@link Locale} to use when parsing
     * @return a {@link Number}
     */
    public abstract Number toNumber(String value, String pattern, Locale locale);
}
