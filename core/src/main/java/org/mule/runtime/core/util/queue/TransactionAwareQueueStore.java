/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.store.ObjectStoreException;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Queue implementation that executes operations:
 *  - If there is no transaction context then executes the operation directly to the queue.
 *  - If there is a transaction context then executes the operation through the transaction context.
 *  - During queue dispose a {@link QueueStoreCacheListener} will be notified
 */
public class TransactionAwareQueueStore implements Queue
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private final MuleContext muleContext;
    private final TransactionContextProvider transactionContextProvider;
    private final QueueStore queue;

    public TransactionAwareQueueStore(QueueStore queue, TransactionContextProvider transactionContextProvider, MuleContext muleContext)
    {
        this.queue = queue;
        this.transactionContextProvider = transactionContextProvider;
        this.muleContext = muleContext;
    }

    @Override
    public void put(Serializable item) throws InterruptedException, ObjectStoreException
    {
        offer(item, Long.MAX_VALUE);
    }

    @Override
    public void clear() throws InterruptedException
    {
        if (transactionContextProvider.isTransactional())
        {
            transactionContextProvider.getTransactionalContext().clear(queue);
        }
        else
        {
            queue.clear();
        }
    }

    @Override
    public boolean offer(Serializable item, long timeout)
            throws InterruptedException, ObjectStoreException
    {
        if (transactionContextProvider.isTransactional())
        {
            return transactionContextProvider.getTransactionalContext().offer(queue, item, timeout);
        }
        else
        {
            return queue.offer(item, 0, timeout);
        }
    }

    @Override
    public Serializable take() throws InterruptedException
    {
        return poll(Long.MAX_VALUE);
    }

    @Override
    public void untake(Serializable item) throws InterruptedException, ObjectStoreException
    {
        if (transactionContextProvider.isTransactional())
        {
            transactionContextProvider.getTransactionalContext().untake(queue, item);
        }
        else
        {
            queue.untake(item);
        }
    }

    @Override
    public Serializable poll(long timeout) throws InterruptedException
    {
        try
        {
            if (transactionContextProvider.isTransactional())
            {
                Serializable item = transactionContextProvider.getTransactionalContext().poll(queue, timeout);
                return postProcessIfNeeded(item);
            }
            else
            {
                return postProcessIfNeeded(queue.poll(timeout));
            }
        }
        catch (InterruptedException iex)
        {
            if (!muleContext.isStopping())
            {
                throw iex;
            }
            // if stopping, ignore
            return null;
        }
    }

    @Override
    public Serializable peek() throws InterruptedException
    {
        if (transactionContextProvider.isTransactional())
        {
            Serializable item = transactionContextProvider.getTransactionalContext().peek(queue);
            return postProcessIfNeeded(item);
        }
        else
        {
            return postProcessIfNeeded(queue.peek());
        }
    }

    @Override
    public void dispose() throws MuleException, InterruptedException
    {
        queue.dispose();
    }

    @Override
    public int size()
    {
        if (transactionContextProvider.isTransactional())
        {
            return transactionContextProvider.getTransactionalContext().size(queue);
        }
        else
        {
            return queue.getSize();
        }
    }

    @Override
    public String getName()
    {
        return queue.getName();
    }

    /**
     * Note -- this must handle null items
     */
    private Serializable postProcessIfNeeded(Serializable item)
    {
        try
        {
            if (item instanceof DeserializationPostInitialisable)
            {
                DeserializationPostInitialisable.Implementation.init(item, muleContext);
            }
            return item;
        }
        catch (Exception e)
        {
            logger.warn("Unable to deserialize message", e);
            return null;
        }
    }
}
