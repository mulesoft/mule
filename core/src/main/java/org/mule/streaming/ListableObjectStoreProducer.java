/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.MuleException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link Producer} to stream the contents of a
 * {@link org.mule.api.store.ListableObjectStore}
 */
public class ListableObjectStoreProducer<T extends Serializable> implements Producer<T>
{

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
            List<Serializable> allKeys = objectStore.allKeys();
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
    @SuppressWarnings("unchecked")
    public List<T> produce()
    {
        if (this.objectStore == null || !this.keys.hasNext())
        {
            return Collections.emptyList();
        }

        try
        {
            return Arrays.asList(this.objectStore.retrieve(this.keys.next()));
        }
        catch (ObjectStoreException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int totalAvailable()
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
