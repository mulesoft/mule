/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.MuleException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Producer} to stream the contents of a
 * {@link ListableObjectStore}
 * 
 * @since 3.5.0
 */
public class ListableObjectStoreProducer<T extends Serializable> implements Producer<T>
{

    private static final Logger logger = LoggerFactory.getLogger(ListableObjectStoreProducer.class);

    private ListableObjectStore<T> objectStore;
    private Iterator<Serializable> keys;
    private int size;

    public ListableObjectStoreProducer(ListableObjectStore<T> objectStore)
    {
        if (objectStore == null)
        {
            throw new IllegalArgumentException("Cannot construct a producer with a null object store");
        }

        this.objectStore = objectStore;
        try
        {
            List<Serializable> allKeys = new ArrayList<Serializable>(objectStore.allKeys());
            this.keys = allKeys.iterator();
            this.size = allKeys.size();
        }
        catch (ObjectStoreException e)
        {
            throw new RuntimeException(
                "Could not construct producer because exception was found retrieving keys", e);
        }
    }

    @Override
    public T produce()
    {
        if (this.objectStore == null || !this.keys.hasNext())
        {
            return null;
        }

        Serializable key = this.keys.next();
        try
        {
            return this.objectStore.retrieve(key);
        }
        catch (ObjectDoesNotExistException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format(
                    "key %s no longer available in objectstore. This is likely due to a concurrency issue. Will continue with next key if available",
                    key));
            }

            return this.produce();
        }
        catch (ObjectStoreException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public void close() throws MuleException
    {
        this.objectStore = null;
        this.keys = null;
    }

}
