/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.internal.util.journal.queue.XaQueueTxJournalEntry;
import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.xa.Xid;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation of {@link XaQueueTransactionContext} for persistent queues using XA
 * transactions
 */
public class PersistentXaTransactionContext implements XaQueueTransactionContext {

  private final XaTxQueueTransactionJournal transactionJournal;
  private final QueueProvider queueProvider;
  private Xid xid;

  public PersistentXaTransactionContext(XaTxQueueTransactionJournal simpleTxQueueTransactionJournal, QueueProvider queueProvider,
                                        Xid xid) {
    this.transactionJournal = simpleTxQueueTransactionJournal;
    this.queueProvider = queueProvider;
    this.xid = xid;
  }

  public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException {
    this.transactionJournal.logAdd(xid, queue, item);
    return true;
  }

  public void untake(QueueStore queue, Serializable item) throws InterruptedException {
    this.transactionJournal.logAddFirst(xid, queue, item);
  }

  public void clear(QueueStore queue) throws InterruptedException {
    synchronized (queue) {
      while (poll(queue, 100) != null);
    }
  }

  public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException {
    synchronized (queue) {
      Serializable value = queue.peek();
      if (value == null) {
        return null;
      }
      this.transactionJournal.logRemove(xid, queue, value);
      return queue.poll(pollTimeout);
    }
  }

  public Serializable peek(QueueStore queue) throws InterruptedException {
    return queue.peek();
  }

  public int size(QueueStore queue) {
    final AtomicInteger addSize = new AtomicInteger(0);
    CollectionUtils.forAllDo(this.transactionJournal.getLogEntriesForTx(xid), new Closure() {

      @Override
      public void execute(Object value) {
        if (((XaQueueTxJournalEntry) value).isAdd() || ((XaQueueTxJournalEntry) value).isAddFirst()) {
          addSize.incrementAndGet();
        }
      }
    });
    return queue.getSize() + addSize.get();
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
