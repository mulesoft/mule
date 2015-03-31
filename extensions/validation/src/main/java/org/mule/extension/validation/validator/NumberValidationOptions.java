/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.validator;

import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.param.Optional;

/**
 * A simple object grouping different settings to be
 * used when validating a {@link Number}
 *
 * @since 3.7.0
 */
public final class NumberValidationOptions
{

    /**
     * Value to validate
     */
    @Parameter
    private String value;

    /**
     * The locale to use for the format
     */
    @Parameter
    @Optional(defaultValue = "US")
    private String locale;

    /**
     * The pattern used to format the value
     */
    @Parameter
    @Optional
    private String pattern;

    /**
     * The minimum value
     */
    @Parameter
    @Optional
    private Number minValue;

    /**
     * The maximum value
     */
    @Parameter
    @Optional
    private Number maxValue;

    public NumberValidationOptions()
    {
    }

    public NumberValidationOptions(String value, String locale, String pattern, Number minValue, Number maxValue)
    {
        this.value = value;
        this.locale = locale;
        this.pattern = pattern;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getValue()
    {
        return value;
    }

    public String getLocale()
    {
        return locale;
    }


    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public Number getMinValue()
    {
        return minValue;
    }

    public Number getMaxValue()
    {
        return maxValue;
    }
}
