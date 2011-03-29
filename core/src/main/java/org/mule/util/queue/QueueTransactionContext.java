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

import org.mule.util.xa.AbstractTransactionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class QueueTransactionContext extends AbstractTransactionContext
{
    private final TransactionalQueueManager transactionalQueueManager;
    protected Map<QueueInfo, List<Object>> added;
    // TODO BL-405 this looks pretty unused, can it be removed?
    protected Map<QueueInfo, List<Object>> removed;

    QueueTransactionContext(TransactionalQueueManager transactionalQueueManager)
    {
        super();
        this.transactionalQueueManager = transactionalQueueManager;
    }

    public boolean offer(QueueInfo queue, Object item, long offerTimeout) throws InterruptedException
    {
        readOnly = false;
        initializeAdded();

        List<Object> queueAdded = lookupQueue(queue);
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

    public void untake(QueueInfo queue, Object item) throws InterruptedException
    {
        readOnly = false;
        initializeAdded();

        List<Object> queueAdded = lookupQueue(queue);
        queueAdded.add(item);
    }

    public Object poll(QueueInfo queue, long pollTimeout) throws IOException, InterruptedException
    {
        readOnly = false;
        if (added != null)
        {
            List<Object> queueAdded = added.get(queue);
            if (queueAdded != null)
            {
                return queueAdded.remove(queueAdded.size() - 1);
            }
        }

        Object object;
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
                removed = new HashMap<QueueInfo, List<Object>>();
            }
            List<Object> queueRemoved = removed.get(queue);
            if (queueRemoved == null)
            {
                queueRemoved = new ArrayList<Object>();
                removed.put(queue, queueRemoved);
            }
            queueRemoved.add(object);
            object = transactionalQueueManager.doLoad(queue, object);
        }
        return object;
    }

    public Object peek(QueueInfo queue) throws IOException, InterruptedException
    {
        readOnly = false;
        if (added != null)
        {
            List<Object> queueAdded = added.get(queue);
            if (queueAdded != null)
            {
                return queueAdded.get(queueAdded.size() - 1);
            }
        }
        Object o = queue.peek();
        if (o != null)
        {
            o = transactionalQueueManager.doLoad(queue, o);
        }
        return o;
    }

    public int size(QueueInfo queue)
    {
        int sz = queue.list.size();
        if (added != null)
        {
            List<Object> queueAdded = added.get(queue);
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
            added = new HashMap<QueueInfo, List<Object>>();
        }
    }

    protected List<Object> lookupQueue(QueueInfo queue)
    {
        List<Object> queueAdded = added.get(queue);
        if (queueAdded == null)
        {
            queueAdded = new ArrayList<Object>();
            added.put(queue, queueAdded);
        }
        return queueAdded;
    }
}
