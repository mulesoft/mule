/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;

import java.io.IOException;

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

        public void put(Object item) throws InterruptedException
        {
            offer(item, Long.MAX_VALUE);
        }

        public boolean offer(Object item, long timeout) throws InterruptedException
        {
            if (localContext != null)
            {
                return ((TransactionalQueueManager.QueueTransactionContext) localContext).offer(queue, item,
                    timeout);
            }
            else
            {
                try
                {
                    Object id = queueManager.doStore(queue, item);
                    try
                    {
                        if (!queue.offer(id, 0, timeout))
                        {
                            queueManager.doRemove(queue, item);
                            return false;
                        }
                        else
                        {
                            return true;
                        }
                    }
                    catch (InterruptedException e)
                    {
                        queueManager.doRemove(queue, item);
                        throw e;
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        public Object take() throws InterruptedException
        {
            return poll(Long.MAX_VALUE);
        }

        public Object poll(long timeout) throws InterruptedException
        {
            try
            {
                if (localContext != null)
                {
                    return ((TransactionalQueueManager.QueueTransactionContext) localContext).poll(queue,
                        timeout);
                }
                else
                {
                    Object id = queue.poll(timeout);
                    if (id != null)
                    {
                        Object item = queueManager.doLoad(queue, id);
                        queueManager.doRemove(queue, id);
                        return item;
                    }
                    return null;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public Object peek() throws InterruptedException
        {
            try
            {
                if (localContext != null)
                {
                    return ((TransactionalQueueManager.QueueTransactionContext) localContext).peek(queue);
                }
                else
                {
                    Object id = queue.peek();
                    if (id != null)
                    {
                        return queueManager.doLoad(queue, id);
                    }
                    return null;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public int size()
        {
            if (localContext != null)
            {
                return ((TransactionalQueueManager.QueueTransactionContext) localContext).size(queue);
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
