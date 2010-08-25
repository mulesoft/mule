/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.safehaus.uuid.UUIDGenerator;

/**
 * <code>UUID</code> Generates a UUID using JDK 5. THe reason for this class is that we have changed the UUID impl in the past.
 * using this class makes it easy to switch out implementations
 */
// @ThreadSafe
public final class UUID
{
    private UUID()
    {
        // no go
    }

    public static String getUUID()
    {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Generates incremental and unique ids.
     * 
     * @return an id that will be different from the previous ones.
     */
    public static String getAscendingOrderUUID()
    {
        // The timeBasedUUID will generate strictly incremental unique ids.
        // Sorting all the ids will give us the ids in the order that were
        // generated.
        return UUIDGenerator.getInstance().generateTimeBasedUUID().toString();
    }

}
