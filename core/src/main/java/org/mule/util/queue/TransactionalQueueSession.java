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
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;

import java.io.Serializable;

/**
 * A Queue session that is used to manage the transaction context of a Queue
 */
class TransactionalQueueSession extends DefaultXASession implements QueueSession
{

    protected TransactionalQueueManager queueManager;

    public TransactionalQueueSession(AbstractXAResourceManager resourceManager,
                                     TransactionalQueueManager queueManager)
    {
        super(resourceManager);
        this.queueManager = queueManager;
    }

    public Queue getQueue(String name)
    {
        QueueInfo queue = queueManager.getQueue(name);
        return new QueueImpl(queue);
    }

    protected class QueueImpl implements Queue
    {
        protected QueueInfo queue;

        public QueueImpl(QueueInfo queue)
        {
            this.queue = queue;
        }

        public void put(Serializable item) throws InterruptedException
        {
            offer(item, Long.MAX_VALUE);
        }

        public boolean offer(Serializable item, long timeout) throws InterruptedException
        {
            if (localContext != null)
            {
                return ((QueueTransactionContext) localContext).offer(queue, item, timeout);
            }
            else
            {
                try
                {
                    Serializable id = queueManager.doStore(queue, item);
                    try
                    {
                        if (!queue.offer(id, 0, timeout))
                        {
                            queueManager.doRemove(queue, id);
                            return false;
                        }
                        else
                        {
                            return true;
                        }
                    }
                    catch (InterruptedException e)
                    {
                        queueManager.doRemove(queue, id);
                        throw e;
                    }
                }
                catch (ObjectStoreException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        public Serializable take() throws InterruptedException
        {
            return poll(Long.MAX_VALUE);
        }

        public void untake(Serializable item) throws InterruptedException
        {
            if (localContext != null)
            {
                ((QueueTransactionContext) localContext).untake(queue, item);
            }
            else
            {
                try
                {
                    Serializable id = queueManager.doStore(queue, item);
                    queue.untake(id);
                }
                catch (ObjectStoreException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        public Serializable poll(long timeout) throws InterruptedException
        {
            try
            {
                if (localContext != null)
                {
                    return ((QueueTransactionContext) localContext).poll(queue, timeout);
                }
                else
                {
                    Serializable id = queue.poll(timeout);
                    if (id != null)
                    {
                        Serializable item = queueManager.doLoad(queue, id);
                        queueManager.doRemove(queue, id);
                        return item;
                    }
                    return null;
                }
            }
            catch (InterruptedException iex)
            {
                if (queueManager.getMuleContext().isStopping())
                {
                    throw iex;
                }
                // if stopping, ignore
                return null;
            }
            catch (ObjectStoreException e)
            {
                throw new RuntimeException(e);
            }
        }

        public Serializable peek() throws InterruptedException
        {
            try
            {
                if (localContext != null)
                {
                    return ((QueueTransactionContext) localContext).peek(queue);
                }
                else
                {
                    Serializable id = queue.peek();
                    if (id != null)
                    {
                        return queueManager.doLoad(queue, id);
                    }
                    return null;
                }
            }
            catch (ObjectStoreException e)
            {
                throw new RuntimeException(e);
            }
        }

        public int size()
        {
            if (localContext != null)
            {
                return ((QueueTransactionContext) localContext).size(queue);
            }
            else
            {
                return queue.list.size();
            }
        }

        public String getName()
        {
            return queue.getName();
        }
    }
}
