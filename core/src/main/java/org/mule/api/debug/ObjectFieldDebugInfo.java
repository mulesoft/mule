/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.mule.util.Preconditions.checkArgument;

import java.util.List;

/**
 * Provides debug information for an object's field that contains a value
 * composed of many simple values.
 *
 * @since 3.8.0
 */
public class ObjectFieldDebugInfo extends FieldDebugInfo<List<FieldDebugInfo>>
{
    /**
     * Creates debug information for an object
     *
     * @param name name of the created field. Must be a not blank {@link String}
     * @param type   class of the field. Cannot be null.
     * @param fields a {@link List} containing debug information for the
     *               object's fields. Non null.
     */
    ObjectFieldDebugInfo(String name, Class type, List<FieldDebugInfo> fields)
    {
        super(name, type, fields);

        checkArgument(fields != null, "Fields cannot be null");
    }
}
