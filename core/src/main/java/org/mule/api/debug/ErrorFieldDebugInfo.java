/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.mule.util.Preconditions.checkArgument;

/**
 * Provides debug information for a field when there was an error
 * obtaining the field's information.
 *
 * @since 3.8.0
 */
public class ErrorFieldDebugInfo extends FieldDebugInfo<Throwable>
{

    /**
     * Creates a debug info for a field when an error has occurred
     * obtaining field's information
     *
     * @param name name of the created field. Must be a not blank {@link String}
     * @param type class of the field. Cannot be null.
     * @param error error that prevents to obtain field debug info. Cannot be null
     */
    ErrorFieldDebugInfo(String name, Class type, Throwable error)
    {
        super(name, type, error);
        checkArgument(type != null, "Type cannot be null");
        checkArgument(error != null, "Error cannot be null");
    }
}
