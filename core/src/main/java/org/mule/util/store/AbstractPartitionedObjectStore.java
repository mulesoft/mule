/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractPartitionedObjectStore<T extends Serializable>
    implements PartitionableObjectStore<T>
{
    protected final static String DEFAULT_PARTITION = "DEFAULT_PARTITION";
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void open() throws ObjectStoreException
    {
        open(DEFAULT_PARTITION);
    }

    @Override
    public void close() throws ObjectStoreException
    {
        close(DEFAULT_PARTITION);
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        return allKeys(DEFAULT_PARTITION);
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return contains(key, DEFAULT_PARTITION);
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        store(key, value, DEFAULT_PARTITION);
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return retrieve(key, DEFAULT_PARTITION);
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        return remove(key, DEFAULT_PARTITION);
    }

}
