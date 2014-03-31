/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.ResourceManagerException;

import java.io.Serializable;

/**
 * Default {@link LocalQueueTransactionContext} implementation for a queue.
 */
public class LocalTxQueueTransactionContext extends AbstractTransactionContext implements LocalQueueTransactionContext, QueueTransactionContextFactory<LocalQueueTransactionContext>
{

    private final LocalTxQueueTransactionJournal localTxQueueTransactionJournal;
    private final QueueProvider queueProvider;
    private final QueueTypeTransactionContextAdapter<LocalQueueTransactionContext> delegate;

    public LocalTxQueueTransactionContext(LocalTxQueueTransactionJournal localTxQueueTransactionJournal, QueueProvider queueProvider)
    {
        this.localTxQueueTransactionJournal = localTxQueueTransactionJournal;
        this.queueProvider = queueProvider;
        this.delegate = new QueueTypeTransactionContextAdapter(this);
    }

    @Override
    public LocalQueueTransactionContext createPersistentTransactionContext()
    {
        return new PersistentQueueTransactionContext(localTxQueueTransactionJournal, queueProvider);
    }

    @Override
    public LocalQueueTransactionContext createTransientTransactionContext()
    {
        return new TransientQueueTransactionContext();
    }

    @Override
    public void doCommit() throws ResourceManagerException
    {
        LocalQueueTransactionContext transactionContext = delegate.getTransactionContext();
        if (transactionContext != null)
        {
            transactionContext.doCommit();
        }
    }

    @Override
    public void doRollback() throws ResourceManagerException
    {
        LocalQueueTransactionContext transactionContext = delegate.getTransactionContext();
        if (transactionContext != null)
        {
            transactionContext.doRollback();
        }
    }

    @Override
    public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException
    {
        return delegate.offer(queue, item, offerTimeout);
    }

    @Override
    public void untake(QueueStore queue, Serializable item) throws InterruptedException
    {
        delegate.untake(queue, item);
    }

    @Override
    public void clear(QueueStore queue) throws InterruptedException
    {
        delegate.clear(queue);
    }

    @Override
    public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException
    {
        return delegate.poll(queue, pollTimeout);
    }

    @Override
    public Serializable peek(QueueStore queue) throws InterruptedException
    {
        return delegate.peek(queue);
    }

    @Override
    public int size(QueueStore queue)
    {
        return delegate.size(queue);
    }
}
