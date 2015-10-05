/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.mule.util.Preconditions.checkArgument;

/**
 * Provides debug information for an object's field that contains a simple value.
 *
 * @since 3.8.0
 */
public class SimpleFieldDebugInfo extends FieldDebugInfo<Object>
{

    /**
     * Creates a debug information for a simple field
     *
     * @param name name of the created field. Must be a not blank {@link String}
     * @param type class of the field. Cannot be null.
     * @param value value of the field. Can be null
     */
    SimpleFieldDebugInfo(String name, Class type, Object value)
    {
        super(name, type, value);
        checkArgument(type != null, "Type cannot be null");
    }

}
