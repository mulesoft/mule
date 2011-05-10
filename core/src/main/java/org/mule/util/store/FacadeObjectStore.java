/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;

import java.io.Serializable;
import java.util.List;

/**
 * An object store that is a facade that delegates to a global object store tha is listed in the registry
 */
public abstract class FacadeObjectStore <T extends Serializable> implements ListableObjectStore<T>, MuleContextAware
{
    private String storeName;
    private ListableObjectStore<T> delegate;
    private MuleContext muleContext;

    public FacadeObjectStore(String storeName)
    {
        this.storeName = storeName;
    }
    
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public void open()
        throws ObjectStoreException
    {
        getDelegate().open();
    }

    public void close()
        throws ObjectStoreException
    {
        getDelegate().close();
    }

    public List<Serializable> allKeys()
        throws ObjectStoreException
    {
        return getDelegate().allKeys();
    }

    public boolean contains(Serializable key)
        throws ObjectStoreException
    {
        return getDelegate().contains(key);
    }

    public void store(Serializable key, T value)
        throws ObjectStoreException
    {
        getDelegate().store(key, value);
    }

    public T retrieve(Serializable key)
        throws ObjectStoreException
    {
        return getDelegate().retrieve(key);
    }

    public T remove(Serializable key)
        throws ObjectStoreException
    {
        return getDelegate().remove(key);
    }

    public boolean isPersistent()
    {
        return getDelegate().isPersistent();
    }

    /**
     * Locate the global store in the registry
     */
    private ListableObjectStore<T> getDelegate()
    {
        if (delegate == null)
        {
            delegate = muleContext.getRegistry().lookupObject(storeName);
            if (delegate == null)
            {
                throw new MuleRuntimeException(CoreMessages.objectStoreNotFound(storeName));
            }
        }

        return delegate;
    }
}
