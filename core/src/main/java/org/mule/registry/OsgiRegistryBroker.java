/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.api.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiRegistryBroker extends AbstractRegistryBroker
{
    private BundleContext bundleContext;
    private ServiceTracker registries;
    
    public OsgiRegistryBroker(BundleContext bundleContext)
    {
        super();
        this.bundleContext = bundleContext;
        registries = new ServiceTracker(bundleContext, Registry.class.getName(), null);
        registries.open();
    }

    public void addRegistry(long id, Registry registry)
    {
        // TODO Set ID as an attribute.
        bundleContext.registerService(Registry.class.getName(), registry, null);
    }

    public void removeRegistry(long id)
    {
        // TODO Unregister registry
    }

    protected Collection/*<Registry>*/ getRegistries()
    {
        Object[] services = registries.getServices();
        int servicesCount = services.length;
        ArrayList list = new ArrayList();
        for (int i = 0; i < servicesCount; ++i)
        {
            list.add(services[i]);
        }
        return list;
    }
}
