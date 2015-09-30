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
 * Provides debug information for an object's field.
 *
 * <p/>
 * To provide debug information for a field that is a composed object (ie: is
 * not a simple value as a {@link String}), set this field value to a
 * {@link ObjectDebugInfo} containing the required debug information for those
 * fields.
 *
 * @since 3.8.0
 */
public class FieldDebugInfo
{

    private final String name;
    private final Object value;
    private final Class type;

    /**
     * Creates a new field debug information
     *
     * @param name name of the created field. Must be a not blank {@link String}
     * @param type class of the field. Can be null.
     * @param value value of the field. Can be null
     */
    public FieldDebugInfo(String name, Class type, Object value)
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

    public Class getType()
    {
        return type;
    }

    public Object getValue()
    {
        return value;
    }
}
