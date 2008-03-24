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

import java.util.Collection;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class DefaultRegistryBroker extends AbstractRegistryBroker
{
    private Map/*<Registry>*/ registries = new ConcurrentHashMap();
    
    public void addRegistry(long id, Registry registry)
    {
        registries.put(id, registry);
    }

    public void removeRegistry(long id)
    {
        registries.remove(id);
    }

    protected Collection/*<Registry>*/ getRegistries()
    {
        return registries.values();
    }
}
