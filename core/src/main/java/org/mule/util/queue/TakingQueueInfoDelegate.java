/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;

/**
 * A QueueInfoDelegate that can take objects directly from its store
 */
public interface TakingQueueInfoDelegate extends QueueInfoDelegate
{
    /**
     * Poll the queue for its first member, and, if there is one, remove and return the corresponding object
     * from the object store
     */
    Serializable takeFromObjectStore(long timeout) throws InterruptedException;

    void writeToObjectStore(Serializable data) throws InterruptedException,ObjectStoreException;
}
