/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import org.mule.api.MuleEvent;

import java.util.List;

/**
 * Provides information about an object for debugging purposes
 *
 * @since 3.8.0
 */
public interface Debuggable
{

    /**
     * Returns debug information about the fields of this object.
     *
     * @param event event used while debugging. Non null
     * @return a non null {@link List}
     */
    List<FieldDebugInfo> getDebugInfo(MuleEvent event);
}