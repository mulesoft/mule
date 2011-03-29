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

    QueueTransactionContext(TransactionalQueueManager transactionalQueueManager)
    {
        this.transactionalQueueManager = transactionalQueueManager;
    }

    protected Map added;
    protected Map removed;

    @SuppressWarnings("unchecked")
    public boolean offer(QueueInfo queue, Object item, long timeout) throws InterruptedException
    {
        readOnly = false;
        if (added == null)
        {
            added = new HashMap();
        }
        List queueAdded = (List) added.get(queue);
        if (queueAdded == null)
        {
            queueAdded = new ArrayList();
            added.put(queue, queueAdded);
        }
        // wait for enough room
        if (queue.offer(null, queueAdded.size(), timeout))
        {
            queueAdded.add(item);
            return true;
        }
        else
        {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public void untake(QueueInfo queue, Object item) throws InterruptedException
    {
        readOnly = false;
        if (added == null)
        {
            added = new HashMap();
        }
        List queueAdded = (List) added.get(queue);
        if (queueAdded == null)
        {
            queueAdded = new ArrayList();
            added.put(queue, queueAdded);
        }
        queueAdded.add(item);
    }

    @SuppressWarnings("unchecked")
    public Object poll(QueueInfo queue, long timeout) throws IOException, InterruptedException
    {
        readOnly = false;
        if (added != null)
        {
            List queueAdded = (List)added.get(queue);
            if (queueAdded != null)
            {
                return queueAdded.remove(queueAdded.size() - 1);
            }
        }
        Object o;
        try
        {
            o = queue.poll(timeout);
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
        if (o != null)
        {
            if (removed == null)
            {
                removed = new HashMap();
            }
            List queueRemoved = (List) removed.get(queue);
            if (queueRemoved == null)
            {
                queueRemoved = new ArrayList();
                removed.put(queue, queueRemoved);
            }
            queueRemoved.add(o);
            o = transactionalQueueManager.doLoad(queue, o);
        }
        return o;
    }

    public Object peek(QueueInfo queue) throws IOException, InterruptedException
    {
        readOnly = false;
        if (added != null)
        {
            List queueAdded = (List) added.get(queue);
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
            List queueAdded = (List) added.get(queue);
            if (queueAdded != null)
            {
                sz += queueAdded.size();
            }
        }
        return sz;
    }
}
