/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.Collection;

/**
 * A QueueInfo delegates the actual work of processing its queue to one of these.
 */
public interface QueueInfoDelegate
{
    /**
     * append a new member to the end of the queue
     */
    void putNow(Serializable o);

    /**
     * Offer to append a new member to the end of the queue
     */
    boolean offer(Serializable o, int room, long timeout) throws InterruptedException, ObjectStoreException;

    /**
     * Poll the queue for its first member, and, if there is one, remove and return it
     */
    Serializable poll(long timeout) throws InterruptedException;

    /**
     * return, but do not remove, the first member of the queue
     */
    Serializable peek() throws InterruptedException;

    /**
     * Restore a previously removed member to the front of the queue
     */
    void untake(Serializable item) throws InterruptedException, ObjectStoreException;

    /**
     * Return the size of the queue
     */
    int getSize();

    /**
     * Appends all of the elements in the specified collection to the queue (optional
     * operation).
     * 
     * @param items to be added to the queue
     * @return <tt>true</tt> if this queue changed as a result of the call
     */
    boolean addAll(Collection<? extends Serializable> items);
}
