/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.api.NamedObject;
import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;

/**
 * <code>Queue</code> TODO
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

}
