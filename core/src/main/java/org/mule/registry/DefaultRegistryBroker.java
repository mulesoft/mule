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

import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

public class DefaultRegistryBroker extends AbstractRegistryBroker
{
    private List/*<Registry>*/ registries = new CopyOnWriteArrayList();
    
    public void addRegistry(Registry registry)
    {
        registries.add(registry);
    }

    public void removeRegistry(Registry registry)
    {
        registries.remove(registry);
    }

    protected List/*<Registry>*/ getRegistries()
    {
        return registries;
    }
}
