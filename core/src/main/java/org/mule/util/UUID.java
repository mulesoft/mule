/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.safehaus.uuid.EthernetAddress;
import org.safehaus.uuid.UUIDGenerator;

/**
 * <code>UUID</code> Generates a UUID using the <a href="http://jug.safehaus.org/">Safehaus UUID generator</a>
 * rather than the built-in version of JDK5. In our performance tests we found the Java version
 * to be blocking much more than the Safehaus one.
 */
// @ThreadSafe
public final class UUID
{
    private static final UUIDGenerator generator = UUIDGenerator.getInstance();
    private static final EthernetAddress dummyAddress = generator.getDummyAddress();

    private UUID()
    {
        // no go
    }

    public static String getUUID()
    {
        return generator.generateTimeBasedUUID(dummyAddress).toString();
    }
}
