/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.store.ObjectStoreException;
import org.mule.util.xa.AbstractTransactionContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueTransactionContext extends AbstractTransactionContext
{
    private final TransactionalQueueManager transactionalQueueManager;
    protected Map<QueueInfo, List<Serializable>> added;
    protected Map<QueueInfo, List<Serializable>> removed;

    public QueueTransactionContext(TransactionalQueueManager transactionalQueueManager)
    {
        super();
        this.transactionalQueueManager = transactionalQueueManager;
    }

    public boolean offer(QueueInfo queue, Serializable item, long offerTimeout) throws InterruptedException
    {
        readOnly = false;
        initializeAdded();

        List<Serializable> queueAdded = lookupQueue(queue);
        // wait for enough room
        if (queue.offer(null, queueAdded.size(), offerTimeout))
        {
            queueAdded.add(item);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void untake(QueueInfo queue, Serializable item) throws InterruptedException
    {
        readOnly = false;
        initializeAdded();

        List<Serializable> queueAdded = lookupQueue(queue);
        queueAdded.add(item);
    }

    public Serializable poll(QueueInfo queue, long pollTimeout) throws InterruptedException, ObjectStoreException
    {
        readOnly = false;
        if (added != null)
        {
            List<Serializable> queueAdded = added.get(queue);
            if (queueAdded != null)
            {
                return queueAdded.remove(queueAdded.size() - 1);
            }
        }

        Serializable object;
        try
        {
            object = queue.poll(pollTimeout);
        }
        catch (InterruptedException e)
        {
            if (transactionalQueueManager.getMuleContext().isStopping())
            {
                throw e;
            }
            // if disposing, ignore
            return null;
        }

        if (object != null)
        {
            if (removed == null)
            {
                removed = new HashMap<QueueInfo, List<Serializable>>();
            }
            List<Serializable> queueRemoved = removed.get(queue);
            if (queueRemoved == null)
            {
                queueRemoved = new ArrayList<Serializable>();
                removed.put(queue, queueRemoved);
            }
            queueRemoved.add(object);
            object = transactionalQueueManager.doLoad(queue, object);
        }
        return object;
    }

    public Serializable peek(QueueInfo queue) throws InterruptedException, ObjectStoreException
    {
        readOnly = false;
        if (added != null)
        {
            List<Serializable> queueAdded = added.get(queue);
            if (queueAdded != null)
            {
                return queueAdded.get(queueAdded.size() - 1);
            }
        }

        Serializable o = queue.peek();
        if (o != null)
        {
            o = transactionalQueueManager.doLoad(queue, o);
        }
        return o;
    }

    public int size(QueueInfo queue)
    {
        int sz = queue.getSize();
        if (added != null)
        {
            List<Serializable> queueAdded = added.get(queue);
            if (queueAdded != null)
            {
                sz += queueAdded.size();
            }
        }
        return sz;
    }

    protected void initializeAdded()
    {
        if (added == null)
        {
            added = new HashMap<QueueInfo, List<Serializable>>();
        }
    }

    protected List<Serializable> lookupQueue(QueueInfo queue)
    {
        List<Serializable> queueAdded = added.get(queue);
        if (queueAdded == null)
        {
            queueAdded = new ArrayList<Serializable>();
            added.put(queue, queueAdded);
        }
        return queueAdded;
    }
}
