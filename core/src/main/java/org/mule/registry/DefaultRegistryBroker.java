/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.registry.InitialisingRegistry;
import org.mule.api.registry.Registry;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @deprecated as of 3.7.0. This will be removed in Mule 4.0
 */
@Deprecated
public class DefaultRegistryBroker extends AbstractRegistryBroker
{
    private final List<Registry> registries = new CopyOnWriteArrayList<>();
    private final AtomicReference<InitialisingRegistry> initialisingRegistry = new AtomicReference<>(null);

    public DefaultRegistryBroker(MuleContext context)
    {
        super(context);
    }


    public void addRegistry(Registry registry)
    {
        registries.add(0, registry);
    }

    public void removeRegistry(Registry registry)
    {
        registries.remove(registry);
        if (registry instanceof InitialisingRegistry)
        {
            initialisingRegistry.compareAndSet((InitialisingRegistry) registry, null);
        }
    }

    protected Collection<Registry> getRegistries()
    {
        return registries;
    }

    protected InitialisingRegistry getInitialisingRegistry()
    {
        InitialisingRegistry initialising = initialisingRegistry.get();
        if (initialising == null)
        {
            for (Registry registry : registries)
            {
                if (registry instanceof InitialisingRegistry)
                {
                    initialising = (InitialisingRegistry) registry;
                    return initialisingRegistry.compareAndSet(null, initialising) ? initialising : initialisingRegistry.get();
                }
            }
        }

        return initialising;
    }
}
