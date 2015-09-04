/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import org.mule.util.collection.ImmutableSetCollector;

import java.util.Set;

/**
 * Utility class for handling capabilities
 *
 * @since 3.7.0
 */
public class CapabilityUtils
{

    private CapabilityUtils()
    {
    }

    /**
     * Returns the items in {@code capabilities} which
     * are instances of {@code capabilityType}
     *
     * @param capabilities   a {@link Set} of capabilities
     * @param capabilityType the type of the capabilities you seek
     * @return a sub {@link Set} of {@code capabilities}
     */
    public static <T> Set<T> getCapabilities(Set<?> capabilities, Class<T> capabilityType)
    {
        return (Set<T>) capabilities.stream().filter(capability -> capabilityType.isInstance(capability)).collect(new ImmutableSetCollector<>());
    }

    /**
     * Returns {@code true} if {code capabilities} contains at least
     * one item which is an instance of {@code capabilityType}
     *
     * @param capabilities   a {@link Set} of capabilities
     * @param capabilityType the type of the capabilities you seek
     * @return {@code true} if at least one of the capabilities is of type {@code capabilityType}. {@code false} otherwise
     */
    public static boolean isCapableOf(Set<?> capabilities, Class<?> capabilityType)
    {
        return !getCapabilities(capabilities, capabilityType).isEmpty();
    }

}
