/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.util.journal.queue.LocalQueueTxJournalEntry;
import org.mule.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.util.xa.ResourceManagerException;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;

/**
 * {@link LocalQueueTransactionContext} implementation for a persistent queue.
 */
public class PersistentQueueTransactionContext implements LocalQueueTransactionContext
{
    private static int lastId = 0;
    private final LocalTxQueueTransactionJournal transactionJournal;
    private final QueueProvider queueProvider;
    private int txId;

    public PersistentQueueTransactionContext(LocalTxQueueTransactionJournal localTxQueueTransactionJournal, QueueProvider queueProvider)
    {
        this.transactionJournal = localTxQueueTransactionJournal;
        this.queueProvider = queueProvider;
        this.txId = getNextId();
    }

    private synchronized static int getNextId()
    {
        int nextId = lastId++;
        if (nextId < 0)
        {
            nextId = lastId = 0;
        }
        return nextId;
    }

    public boolean offer(QueueStore queue, Serializable item, long offerTimeout)
        throws InterruptedException
    {
        this.transactionJournal.logAdd(txId, queue, item);
        return true;
    }

    public void untake(QueueStore queue, Serializable item) throws InterruptedException
    {
        this.transactionJournal.logAddFirst(txId, queue, item);
    }

    public void clear(QueueStore queue) throws InterruptedException
    {
        synchronized (queue)
        {
            while (poll(queue, 10) != null);
        }
    }

    public Serializable poll(QueueStore queue, long pollTimeout)
        throws InterruptedException
    {
        synchronized (queue)
        {
            Serializable value = queue.peek();
            if (value == null)
            {
                return null;
            }
            this.transactionJournal.logRemove(txId, queue, value);
            return queue.poll(pollTimeout);
        }
    }

    public Serializable peek(QueueStore queue) throws InterruptedException
    {
        return queue.peek();
    }

    public int size(final QueueStore queue)
    {
        final AtomicInteger addSize = new AtomicInteger(0);
        CollectionUtils.forAllDo(this.transactionJournal.logEntriesForTx(txId), new Closure()
        {
            @Override
            public void execute(Object value)
            {
                LocalQueueTxJournalEntry localTxJournalEntry = (LocalQueueTxJournalEntry) value;
                if (localTxJournalEntry.getQueueName().equals(queue.getName()) && (localTxJournalEntry.isAdd() ||  localTxJournalEntry.isAddFirst()))
                {
                    addSize.incrementAndGet();
                }
            }
        });
        return queue.getSize() + addSize.get();
    }

    @Override
    public void doCommit() throws ResourceManagerException
    {
        try
        {
            Collection<LocalQueueTxJournalEntry> logEntries = this.transactionJournal.logEntriesForTx(txId);
            for (LocalQueueTxJournalEntry entry : logEntries)
            {
                if (entry.isAdd())
                {
                    queueProvider.getQueue(entry.getQueueName()).putNow(entry.getValue());
                }
                else if (entry.isAddFirst())
                {
                    queueProvider.getQueue(entry.getQueueName()).untake(entry.getValue());
                }
            }
            this.transactionJournal.logCommit(txId);
        }
        catch (Exception e)
        {
            throw new ResourceManagerException(e);
        }
    }

    @Override
    public void doRollback() throws ResourceManagerException
    {
        Collection<LocalQueueTxJournalEntry> logEntries = this.transactionJournal.logEntriesForTx(txId);
        for (LocalQueueTxJournalEntry entry : logEntries)
        {
            if (entry.isRemove())
            {
                try
                {
                    queueProvider.getQueue(entry.getQueueName()).putNow(entry.getValue());
                }
                catch (InterruptedException e)
                {
                    throw new ResourceManagerException(e);
                }
            }
        }
        this.transactionJournal.logRollback(txId);
    }

}
