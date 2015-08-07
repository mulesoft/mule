/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.registry.LifecycleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.registry.RegistryBroker;
import org.mule.api.registry.RegistryProvider;
import org.mule.lifecycle.RegistryBrokerLifecycleManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @deprecated as of 3.7.0. This will be removed in Mule 4.0
 */
@Deprecated
public abstract class AbstractRegistryBroker implements RegistryBroker, RegistryProvider
{

    protected RegistryBrokerLifecycleManager lifecycleManager;


    public AbstractRegistryBroker(MuleContext muleContext)
    {
        lifecycleManager = new RegistryBrokerLifecycleManager("mule.registry.broker", this, muleContext);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        lifecycleManager.fireInitialisePhase(new LifecycleCallback<AbstractRegistryBroker>()
        {
            public void onTransition(String phaseName, AbstractRegistryBroker broker) throws MuleException
            {
                for (Registry registry : broker.getRegistries())
                {
                    registry.initialise();
                }
            }
        });
    }

    @Override
    public void dispose()
    {
        lifecycleManager.fireDisposePhase(new LifecycleCallback<AbstractRegistryBroker>()
        {
            public void onTransition(String phaseName, AbstractRegistryBroker broker) throws MuleException
            {
                for (Registry registry : broker.getRegistries())
                {
                    registry.dispose();
                }
            }
        });
    }

    @Override
    public void fireLifecycle(String phase) throws LifecycleException
    {
        if (Initialisable.PHASE_NAME.equals(phase))
        {
            initialise();
        }
        else if (Disposable.PHASE_NAME.equals(phase))
        {
            dispose();
        }
        else
        {
            lifecycleManager.fireLifecycle(phase);
            for (Registry registry : getRegistries())
            {
                registry.fireLifecycle(phase);
            }
        }

    }

    @Override
    public String getRegistryId()
    {
        return this.toString();
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    @Override
    public boolean isRemote()
    {
        return false;
    }

     ////////////////////////////////////////////////////////////////////////////////
   // Delegating methods
    ////////////////////////////////////////////////////////////////////////////////


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key)
    {
        return (T) lookupObject(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lookupObject(String key)
    {
        Object obj = null;
        for (Registry registry : getRegistries())
        {
            obj = registry.lookupObject(key);
            if (obj != null)
            {
                break;
            }
        }

        return (T) obj;
    }

    /**
     * Iterates through {@link #getRegistries()} trying to
     * find the first one which is an instance of
     * {@link LifecycleRegistry}. When found, it returns
     * the result of {@link LifecycleRegistry#lookupObject(String, boolean)}.
     * <p/>
     * If none of the available registries is of that type, then it
     * fallbacks to {@link #lookupObject(String)}
     *
     * @param key            the key of the object you're looking for
     * @param applyLifecycle whether lifecycle should be applied to the object before returning
     * @param <T>            the type of the expected object
     * @return the object registered under {@code key}
     */
    @SuppressWarnings("unchecked")
    public <T> T lookupObject(String key, boolean applyLifecycle)
    {
        for (Registry registry : getRegistries())
        {
            if (registry instanceof LifecycleRegistry)
            {
                Object obj = ((LifecycleRegistry) registry).lookupObject(key, applyLifecycle);
                if (obj != null)
                {
                    return (T) obj;
                }
            }
        }

        return lookupObject(key);
    }

    @Override
    public <T> T lookupObject(Class<T> type) throws RegistrationException
    {
        for (Registry registry : getRegistries())
        {
            Object object = registry.lookupObject(type);
            if (object != null)
            {
                return (T) object;
            }
        }
        return null;
    }

    @Override
    public <T> Collection<T> lookupObjects(Class<T> type)
    {
        Collection<T> objects = new ArrayList<>();

        for (Registry registry : getRegistries())
        {
            objects.addAll(registry.lookupObjects(type));
        }

        return objects;
    }

    @Override
    public <T> Collection<T> lookupLocalObjects(Class<T> type)
    {
        Collection<T> objects = new ArrayList<>();
        for (Registry registry : getRegistries())
        {
            objects.addAll(registry.lookupLocalObjects(type));
        }

        return objects;
    }

    @Override
    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        Map<String, T> results = new HashMap<>();
        for (Registry registry : getRegistries())
        {
            results.putAll(registry.lookupByType(type));
        }

        return results;
    }

    @Override
    public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type)
    {
        Collection<T> objects = new ArrayList<>();
        for (Registry registry : getRegistries())
        {
            objects.addAll(registry.lookupObjectsForLifecycle(type));
        }

        return objects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerObject(String key, Object value) throws RegistrationException
    {
        for (Registry registry : getRegistries())
        {
            if (!registry.isReadOnly())
            {
                registry.registerObject(key, value);
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        registerObject(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerObjects(Map objects) throws RegistrationException
    {
        for (Entry<String, Object> entry : (Set<Entry<String, Object>>) objects.entrySet())
        {
            registerObject(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unregisterObject(String key) throws RegistrationException
    {
        for (Registry registry : getRegistries())
        {
            if (!registry.isReadOnly() && registry.lookupObject(key) != null)
            {
                return registry.unregisterObject(key);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Object unregisterObject(String key, Object metadata) throws RegistrationException
    {
        return unregisterObject(key);
    }
}
