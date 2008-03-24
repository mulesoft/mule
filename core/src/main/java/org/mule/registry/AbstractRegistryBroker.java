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

import org.mule.MuleServer;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.registry.RegistryBroker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractRegistryBroker implements RegistryBroker
{
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        // We start off with the TransientRegistry by default.
        addRegistry(-1, new TransientRegistry());
        
        Iterator it = getRegistries().iterator(); 
        while (it.hasNext())
        {
            ((Registry) it.next()).initialise();
        }
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {
        Iterator it = getRegistries().iterator(); 
        while (it.hasNext())
        {
            ((Registry) it.next()).dispose();
        }
    }

    public String getRegistryId()
    {
        return this.toString();
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public boolean isRemote()
    {
        return false;
    }

    abstract protected Collection/*<Registry>*/ getRegistries();
    
    ////////////////////////////////////////////////////////////////////////////////
    // Delegating methods
    ////////////////////////////////////////////////////////////////////////////////
    
    public Object lookupObject(String key)
    {
        Object obj = null;
        Iterator it = getRegistries().iterator(); 
        while (obj == null && it.hasNext())
        {
            obj = ((Registry) it.next()).lookupObject(key);
        }
        return obj;
    }

    public Object lookupObject(Class type) throws RegistrationException
    {
        // Accumulate objects from all registries.
        Collection objects = lookupObjects(type);
        
        if (objects.size() == 1)
        {
            return objects.iterator().next();
        }
        else if (objects.size() > 1)
        {
            throw new RegistrationException("More than one object of type " + type + " registered but only one expected.");
        }
        else
        {
            return null;
        }
    }

    public Collection lookupObjects(Class type)
    {
        Collection objects = new ArrayList();
        
        Iterator it = getRegistries().iterator(); 
        while (it.hasNext())
        {
            objects.addAll(((Registry) it.next()).lookupObjects(type));
        }
        return objects;
    }

    public void registerObject(String key, Object value) throws RegistrationException
    {
        registerObject(key, value, null);
    }

    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        if (value instanceof MuleContextAware)
        {
            ((MuleContextAware) value).setMuleContext(MuleServer.getMuleContext());
        }
        
        Iterator it = getRegistries().iterator(); 
        Registry reg;
        while (it.hasNext())
        {
            reg = (Registry) it.next();
            if (!reg.isReadOnly())
            {
                reg.registerObject(key, value, metadata);
                break;
            }
        }
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        Iterator it = objects.keySet().iterator();
        Object key;
        while (it.hasNext())
        {
            key = it.next();
            registerObject((String) key, objects.get(key));
        }
    }

    public void unregisterObject(String key) throws RegistrationException
    {
        unregisterObject(key, null);    
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        Iterator it = getRegistries().iterator(); 
        Registry reg;
        while (it.hasNext())
        {
            reg = (Registry) it.next();
            if (!reg.isReadOnly() && reg.lookupObject(key) != null)
            {
                reg.unregisterObject(key, metadata);
                break;
            }
        }
    }    
}
