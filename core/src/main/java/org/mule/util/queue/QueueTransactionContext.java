/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.api.store.ObjectStoreException;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.ResourceManagerException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueTransactionContext extends AbstractTransactionContext
{
    private final TransactionalQueueManager transactionalQueueManager;
    private Map<QueueInfo, List<Serializable>> added;
    private Map<QueueInfo, List<Serializable>> removed;

    public QueueTransactionContext(TransactionalQueueManager transactionalQueueManager)
    {
        super();
        this.transactionalQueueManager = transactionalQueueManager;
    }

    public boolean offer(QueueInfo queue, Serializable item, long offerTimeout) throws InterruptedException, ObjectStoreException
    {
        readOnly = false;
        if (queue.canTakeFromStore())
        {
            queue.writeToObjectStore(item);
            return true;
        }

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

    public void untake(QueueInfo queue, Serializable item) throws InterruptedException, ObjectStoreException
    {
        readOnly = false;
        if (queue.canTakeFromStore())
        {
            queue.writeToObjectStore(item);
        }

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
            if (queueAdded != null && queueAdded.size() > 0)
            {
                return queueAdded.remove(queueAdded.size() - 1);
            }
        }

        if (queue.canTakeFromStore())
        {
            // TODO: verify that the queue is transactional too
            return queue.takeNextItemFromStore(timeout);
        }

        Serializable key;
        Serializable value = null;
        try
        {
            key = queue.poll(pollTimeout);
        }
        catch (InterruptedException e)
        {
            if (!transactionalQueueManager.getMuleContext().isStopping())
            {
                throw e;
            }
            // if disposing, ignore
            return null;
        }

        if (key != null)
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
            value = transactionalQueueManager.doLoad(queue, key);
            if (value != null)
            {
                queueRemoved.add(key);
            }
        }
        return value;
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

    @Override
    public void doCommit() throws ResourceManagerException
    {
        try
        {
            if (added != null)
            {
                for (Map.Entry<QueueInfo, List<Serializable>> entry : added.entrySet())
                {
                    QueueInfo queue = entry.getKey();
                    List<Serializable> queueAdded = entry.getValue();
                    if (queueAdded != null && queueAdded.size() > 0)
                    {
                        for (Serializable object : queueAdded)
                        {
                            Serializable id = transactionalQueueManager.doStore(queue, object);
                            queue.putNow(id);
                        }
                    }
                }
            }
            if (removed != null)
            {
                for (Map.Entry<QueueInfo, List<Serializable>> entry : removed.entrySet())
                {
                    QueueInfo queue = entry.getKey();
                    List<Serializable> queueRemoved = entry.getValue();
                    if (queueRemoved != null && queueRemoved.size() > 0)
                    {
                        for (Serializable id : queueRemoved)
                        {
                            transactionalQueueManager.doRemove(queue, id);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new ResourceManagerException(e);
        }
        finally
        {
            added = null;
            removed = null;
        }

    }

    @Override
    public void doRollback() throws ResourceManagerException
    {
        if (removed != null)
        {
            for (Map.Entry<QueueInfo, List<Serializable>> entry : removed.entrySet())
            {
                QueueInfo queue = entry.getKey();
                List<Serializable> queueRemoved = entry.getValue();
                if (queueRemoved != null && queueRemoved.size() > 0)
                {
                    for (Serializable id : queueRemoved)
                    {
                        queue.putNow(id);
                    }
                }
            }
        }
        added = null;
        removed = null;
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
