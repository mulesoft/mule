/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ManagedObjectStore<T extends Serializable> implements ListableObjectStore<T>, MuleContextAware
{
    String storeName;
    boolean isPersistent;
    ListableObjectStore<T> store;
    MuleContext context;
    int maxEntries = 0;
    int entryTTL;
    int expirationInterval;

    public String getStoreName()
    {
        return storeName;
    }

    public void setStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    public boolean isPersistent()
    {
        return isPersistent;
    }

    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }

    public int getMaxEntries()
    {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries)
    {
        this.maxEntries = maxEntries;
    }

    public int getEntryTTL()
    {
        return entryTTL;
    }

    public void setEntryTTL(int entryTTL)
    {
        this.entryTTL = entryTTL;
    }

    public int getExpirationInterval()
    {
        return expirationInterval;
    }

    public void setExpirationInterval(int expirationInterval)
    {
        this.expirationInterval = expirationInterval;
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return getStore().contains(key);
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        getStore().store(key, value);
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return getStore().retrieve(key);
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        return getStore().remove(key);
    }

    @Override
    public void open() throws ObjectStoreException
    {
        ListableObjectStore<T> store = getStore();
        if (store != null)
        {
            store.open();
        }
    }

    @Override
    public void close() throws ObjectStoreException
    {
        ListableObjectStore<T> store = getStore();
        if (store != null)
        {
            getStore().close();
        }
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        ListableObjectStore<T> store = getStore();
        if (store != null)
        {
            return store.allKeys();
        }
        return new ArrayList<Serializable>();
    }

    private ListableObjectStore<T> getStore()
    {
        if (store == null)
        {
            ObjectStoreManager objectStoreManager = (ObjectStoreManager) context.getRegistry().lookupObject(
                MuleProperties.OBJECT_STORE_MANAGER);
            if (objectStoreManager == null)
            {
                return null;
            }
            if (maxEntries != 0)
            {
                store = (ListableObjectStore<T>) objectStoreManager.getObjectStore(storeName, isPersistent,
                    maxEntries, entryTTL, expirationInterval);
            }
            else
            {
                store = (ListableObjectStore<T>) objectStoreManager.getObjectStore(storeName, isPersistent);
            }
        }
        return store;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

}
