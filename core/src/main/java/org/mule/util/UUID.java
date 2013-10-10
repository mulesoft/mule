/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

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

    private UUID()
    {
        // no go
    }

    public static String getUUID()
    {
        return generator.generateTimeBasedUUID().toString();
    }
}
