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

import java.util.Comparator;

import org.safehaus.uuid.UUIDGenerator;

/**
 * <code>UUID</code> Generates a UUID using the JUG library
 */
// @ThreadSafe
public final class UUID
{
    private static final UUIDGenerator generator = UUIDGenerator.getInstance();
    private static final Comparator comparator = new UUIDComparator();

    private static class UUIDComparator implements Comparator
    {
        public int compare(Object uuid, Object other)
        {
            // silly temporary implementation until I have re-implemented the original :(
            return new org.safehaus.uuid.UUID((String) uuid).compareTo(new org.safehaus.uuid.UUID(
                (String) other));
        }
    }

    private UUID()
    {
        // no go
    }

    public static String getUUID()
    {
        return generator.generateTimeBasedUUID().toString();
    }

    public static Comparator getComparator()
    {
        return comparator;
    }

}
