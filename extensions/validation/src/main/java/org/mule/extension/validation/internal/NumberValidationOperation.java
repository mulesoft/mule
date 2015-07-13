/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.api.MuleEvent;
import org.mule.extension.annotations.Operation;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.annotations.param.UseConfig;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.internal.validator.NumberType;
import org.mule.extension.validation.internal.validator.NumberValidator;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 * Defines operations to validate numbers
 *
 * @since 3.7.0
 */
public class NumberValidationOperation extends ValidationSupport
{

    /**
     * Receives a numeric {@code value} as a {@link String} and validates that it can be parsed
     * per the rules of a {@code numberType}
     *
     * @param value      the value to be tested
     * @param locale     The locale to use for the format. If not provided it defaults to the system {@link Locale}
     * @param pattern    The pattern used to format the value
     * @param minValue   If provided, check that the parsed value is greater or equal than this value
     * @param maxValue   If provided, check that the parsed value is less or equal than this value
     * @param numberType the type of number to test {@code value} against
     * @param options    the {@link ValidationOptions}
     * @param event      the current {@link MuleEvent}
     */
    @Operation
    public void isNumber(String value,
                         @Optional String locale,
                         @Optional String pattern,
                         @Optional String minValue,
                         @Optional String maxValue,
                         NumberType numberType,
                         @ParameterGroup ValidationOptions options,
                         MuleEvent event,
                         @UseConfig ValidationExtension config) throws Exception
    {

        ValidationContext context = createContext(options, event, config);
        Validator validator = new NumberValidator(value,
                                                  parseLocale(locale),
                                                  pattern,
                                                  parseNumber(minValue, numberType),
                                                  parseNumber(maxValue, numberType),
                                                  numberType,
                                                  context);

        validateWith(validator, context, event);
    }

    private Number parseNumber(String value, NumberType numberType)
    {
        return StringUtils.isBlank(value) ? null : numberType.toNumber(value, null, parseLocale(null));
    }

}
