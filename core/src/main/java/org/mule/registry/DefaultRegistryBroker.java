/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultRegistryBroker extends AbstractRegistryBroker
{
    private TransientRegistry transientRegistry;
    private List<Registry> registries = new ArrayList<Registry>();

    public DefaultRegistryBroker(MuleContext context)
    {
        super(context);
        transientRegistry = new TransientRegistry(context);
        registries.add(0, transientRegistry);
    }

    TransientRegistry getTransientRegistry()
    {
        return transientRegistry;
    }

    public void addRegistry(Registry registry)
    {
        registries.add(1, registry);
    }

    public void removeRegistry(Registry registry)
    {
        registries.remove(registry);
    }

    protected Collection<Registry> getRegistries()
    {
        return registries;
    }
}
