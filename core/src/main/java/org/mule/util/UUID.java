/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

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

}
