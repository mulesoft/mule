/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.Capable;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

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
        ImmutableSet.Builder<T> matches = ImmutableSet.builder();
        for (Object capability : capabilities)
        {
            if (capabilityType.isInstance(capability))
            {
                matches.add((T) capability);
            }
        }

        return matches.build();
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


    /**
     * Expects {@code capable} to have at most one capability of type {@code capabilityType}
     * and returns such capability.
     * If {@code capable} doesn't have a matching capability, then it returns {@code null}.
     * If {@code capable} has more than one instance of that capability, then it throws
     * {@link IllegalArgumentException}
     *
     * @param capable        a {@link Capable}
     * @param capabilityType a capability t ype
     * @param <T>            the generic type of the capability you're looking for
     * @return A capability or {@code null}
     * @throws IllegalArgumentException if more than one capability of type {@code capabilityType} is found
     */
    public static <T> T getSingleCapability(Capable capable, Class<T> capabilityType)
    {
        Set<T> capabilities = capable.getCapabilities(capabilityType);
        if (CollectionUtils.isEmpty(capabilities))
        {
            return null;
        }

        checkArgument(capabilities.size() == 1, String.format("One instance of capability %s was expected but %d found instead", capabilityType.getName(), capabilities.size()));

        return capabilities.iterator().next();

    }

    /**
     * Expects the {@code capabilities} {@link Set} to have at most one capability of type {@code capabilityType}
     * and returns such capability.
     *
     * @param capabilities   a {@link Set} with capability objects
     * @param capabilityType the {@link Class} of a certain capability type
     * @param <T>            the generic type of the capability you're looking for
     * @return an instance of {@code T} or {@code null} is {@code capabilities} is empty or contains no matching item
     */
    public static <T> T getSingleCapability(Set<?> capabilities, Class<T> capabilityType)
    {
        if (CollectionUtils.isEmpty(capabilities))
        {
            return null;
        }

        for (Object capability : capabilities)
        {
            if (capabilityType.isInstance(capability))
            {
                return (T) capability;
            }
        }

        return null;
    }
}
