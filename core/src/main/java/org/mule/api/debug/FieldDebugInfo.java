/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.util.StringUtils;

/**
 * Provides debug information for an object field.
 *
 * <p/>
 * Debug information is for informative purposes while debugging a Mule application.
 * Obtaining such information can cause collateral effects, for example when an MEL expression
 * is evaluated to obtain the value and then evaluated again on the real execution.
 *
 * @param <T> type of the field's value
 * @since 3.8.0
 */
public abstract class FieldDebugInfo<T>
{

    private final String name;
    private final Class type;
    private final T value;

    /**
     * Creates a new debug info for a field
     *
     * @param name field's name. A non blank {@link String}
     * @param type field's type.
     * @param value field's value. Can be null.
     */
    FieldDebugInfo(String name, Class type, T value)
    {
        checkArgument(!StringUtils.isEmpty(name), "Name cannot be empty");

        this.type = type;
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public T getValue()
    {
        return value;
    }

    public Class getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return String.format("%s[name: '%s', type: '%s', value: '%s']", this.getClass().getSimpleName(), getName(), type, getValue());
    }
}
