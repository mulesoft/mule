/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.queue;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NamedObject;
import org.mule.runtime.core.api.store.ObjectStoreException;

import java.io.Serializable;

/**
 * Interface for mule queues used for VM.
 */
public interface Queue extends NamedObject
{
    /**
     * Returns the number of elements in this queue.
     */
    int size();

    /**
     * Puts a new object in this queue and wait if necessary.
     */
    void put(Serializable object) throws InterruptedException, ObjectStoreException;

    /**
     * Blocks and retrieves an object from this queue.
     * 
     * @return an object.
     */
    Serializable take() throws InterruptedException;

    void untake(Serializable item) throws InterruptedException, ObjectStoreException;

    Serializable peek() throws InterruptedException;

    Serializable poll(long timeout) throws InterruptedException;

    boolean offer(Serializable object, long timeout) throws InterruptedException, ObjectStoreException;

    /**
     * Discards all the elements in the queue
     * 
     * @throws InterruptedException
     */
    public void clear() throws InterruptedException;

    /**
     * Disposes this queue by releasing it's storage and associated memory and
     * storage. If after disposing the queue you try go get it back, you'll get a
     * fresh new one which maintains none of the original one's data
     * 
     * @throws MuleException
     * @throws InterruptedException
     */
    public void dispose() throws MuleException, InterruptedException;

}
