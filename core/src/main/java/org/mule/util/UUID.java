/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.safehaus.uuid.UUIDGenerator;

/**
 * <code>UUID</code> Generates a UUID using the JUG library
 */
// @ThreadSafe
public class UUID
{
    private static final UUIDGenerator generator = UUIDGenerator.getInstance();

    private UUID()
    {
        // no go
    }

    public static String getUUID()
    {
        return generator.generateTimeBasedUUID().toString();
    }

}
