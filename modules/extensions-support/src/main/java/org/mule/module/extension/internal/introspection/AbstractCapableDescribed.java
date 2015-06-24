/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import org.mule.extension.introspection.Capable;
import org.mule.extension.introspection.Described;
import org.mule.module.extension.internal.util.CapabilityUtils;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Abstract implementation for a class that implements both
 * the {@link Described} and
 * {@link Capable} contracts
 *
 * @since 3.7.0
 */
abstract class AbstractCapableDescribed extends AbstractImmutableDescribed implements Capable
{

    private final Set<Object> capabilities;

    AbstractCapableDescribed(String name, String description, Set<Object> capabilities)
    {
        super(name, description);
        this.capabilities = capabilities != null ? ImmutableSet.copyOf(capabilities) : ImmutableSet.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Set<T> getCapabilities(Class<T> capabilityType)
    {
        return CapabilityUtils.getCapabilities(capabilities, capabilityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCapableOf(Class<?> capabilityType)
    {
        return CapabilityUtils.isCapableOf(capabilities, capabilityType);
    }
}
