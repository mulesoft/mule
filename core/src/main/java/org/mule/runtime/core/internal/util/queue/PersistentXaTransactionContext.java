/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.internal.util.journal.queue.XaQueueTxJournalEntry;
import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;

import java.io.Serializable;
import java.util.Collection;

import javax.transaction.xa.Xid;

/**
 * Implementation of {@link XaQueueTransactionContext} for persistent queues using XA transactions
 */
public class PersistentXaTransactionContext implements XaQueueTransactionContext {

  private final XaTxQueueTransactionJournal transactionJournal;
  private final QueueProvider queueProvider;
  private final Xid xid;

  public PersistentXaTransactionContext(XaTxQueueTransactionJournal simpleTxQueueTransactionJournal, QueueProvider queueProvider,
                                        Xid xid) {
    this.transactionJournal = simpleTxQueueTransactionJournal;
    this.queueProvider = queueProvider;
    this.xid = xid;
  }

  @Override
  public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException {
    this.transactionJournal.logAdd(xid, queue, item);
    return true;
  }

  @Override
  public void untake(QueueStore queue, Serializable item) throws InterruptedException {
    this.transactionJournal.logAddFirst(xid, queue, item);
  }

  @Override
  public void clear(QueueStore queue) throws InterruptedException {
    synchronized (queue) {
      while (poll(queue, 100) != null);
    }
  }

  @Override
  public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException {
    synchronized (queue) {
      Serializable value = queue.poll(pollTimeout);
      if (value != null) {
        this.transactionJournal.logRemove(xid, queue, value);
      }
      return value;
    }
  }

  @Override
  public Serializable peek(QueueStore queue) throws InterruptedException {
    return queue.peek();
  }

  @Override
  public int size(QueueStore queue) {
    int addSize = (int) this.transactionJournal.getLogEntriesForTx(xid)
        .stream()
        .filter(value -> value.isAdd() || value.isAddFirst())
        .count();

    return queue.getSize() + addSize;
  }

  @Override
  public void doCommit() throws ResourceManagerException {
    try {
      Collection<XaQueueTxJournalEntry> logEntries = this.transactionJournal.getLogEntriesForTx(xid);
      for (XaQueueTxJournalEntry entry : logEntries) {
        if (entry.isAdd()) {
          queueProvider.getQueue(entry.getQueueName()).putNow(entry.getValue());
        } else if (entry.isAddFirst()) {
          queueProvider.getQueue(entry.getQueueName()).untake(entry.getValue());
        }
      }
      this.transactionJournal.logCommit(xid);
    } catch (Exception e) {
      throw new ResourceManagerException(e);
    }
  }

  @Override
  public void doRollback() throws ResourceManagerException {
    Collection<XaQueueTxJournalEntry> logEntries = this.transactionJournal.getLogEntriesForTx(xid);
    for (XaQueueTxJournalEntry entry : logEntries) {
      if (entry.isRemove()) {
        try {
          queueProvider.getQueue(entry.getQueueName()).putNow(entry.getValue());
        } catch (InterruptedException e) {
          throw new ResourceManagerException(e);
        }
      }
    }
    this.transactionJournal.logRollback(xid);
  }

  @Override
  public void doPrepare() throws ResourceManagerException {
    this.transactionJournal.logPrepare(xid);
  }

}
