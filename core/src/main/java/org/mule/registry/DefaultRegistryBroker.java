/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.registry.LifecycleRegistry;
import org.mule.api.registry.Registry;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated as of 3.7.0. This will be removed in Mule 4.0
 */
@Deprecated
public class DefaultRegistryBroker extends AbstractRegistryBroker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRegistryBroker.class);

    private final List<Registry> registries = new CopyOnWriteArrayList<>();
    private final AtomicReference<LifecycleRegistry> lifecycleRegistry = new AtomicReference<>(null);

    public DefaultRegistryBroker(MuleContext context)
    {
        super(context);
    }


    public void addRegistry(Registry registry)
    {
        registries.add(0, registry);
        if (registries.size() > 1 && LOGGER.isWarnEnabled())
        {
            LOGGER.warn("A second registry has been registered, however adding registries has been deprecated as of Mule 3.7.0");
        }
    }

    public void removeRegistry(Registry registry)
    {
        registries.remove(registry);
        if (registry instanceof LifecycleRegistry)
        {
            lifecycleRegistry.compareAndSet((LifecycleRegistry) registry, null);
        }
    }

    protected Collection<Registry> getRegistries()
    {
        return registries;
    }

    protected LifecycleRegistry getLifecycleRegistry()
    {
        LifecycleRegistry initialising = lifecycleRegistry.get();
        if (initialising == null)
        {
            for (Registry registry : registries)
            {
                if (registry instanceof LifecycleRegistry)
                {
                    initialising = (LifecycleRegistry) registry;
                    return lifecycleRegistry.compareAndSet(null, initialising) ? initialising : lifecycleRegistry.get();
                }
            }
        }

        return initialising;
    }
}
