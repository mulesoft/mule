/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.runtime.core.internal.transaction.xa.AbstractTransactionContext;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;

/**
 * Default {@link LocalQueueTransactionContext} implementation for a queue.
 */
public class LocalTxQueueTransactionContext extends AbstractTransactionContext
    implements LocalQueueTransactionContext, QueueTransactionContextFactory<LocalQueueTransactionContext> {

  private final LocalTxQueueTransactionJournal localTxQueueTransactionJournal;
  private final QueueProvider queueProvider;
  private final QueueTypeTransactionContextAdapter<LocalQueueTransactionContext> delegate;
  private final Lock transactionContextAccessLock;

  public LocalTxQueueTransactionContext(LocalTxQueueTransactionJournal localTxQueueTransactionJournal,
                                        QueueProvider queueProvider, Lock transactionContextAccessLock) {
    this.localTxQueueTransactionJournal = localTxQueueTransactionJournal;
    this.queueProvider = queueProvider;
    this.transactionContextAccessLock = transactionContextAccessLock;
    this.delegate = new QueueTypeTransactionContextAdapter(this);
  }

  @Override
  public LocalQueueTransactionContext createPersistentTransactionContext() {
    return new PersistentQueueTransactionContext(localTxQueueTransactionJournal, queueProvider);
  }

  @Override
  public LocalQueueTransactionContext createTransientTransactionContext() {
    return new TransientQueueTransactionContext();
  }

  @Override
  public void doCommit() throws ResourceManagerException {
    LocalQueueTransactionContext transactionContext = delegate.getTransactionContext();
    if (transactionContext != null) {
      transactionContext.doCommit();
    }
  }

  @Override
  public void doRollback() throws ResourceManagerException {
    LocalQueueTransactionContext transactionContext = delegate.getTransactionContext();
    if (transactionContext != null) {
      transactionContext.doRollback();
    }
  }

  @Override
  public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException {
    long beginMillis = currentTimeMillis();
    final boolean lockAcquired = transactionContextAccessLock.tryLock(offerTimeout, MILLISECONDS);
    if (lockAcquired) {
      try {
        long remainingTimeout = getRemainingTimeout(offerTimeout, beginMillis);
        if (remainingTimeout >= 0) {
          return delegate.offer(queue, item, remainingTimeout);
        }
      } finally {
        transactionContextAccessLock.unlock();
      }
    }
    return false;
  }

  @Override
  public void untake(QueueStore queue, Serializable item) throws InterruptedException {
    transactionContextAccessLock.lock();
    try {
      delegate.untake(queue, item);
    } finally {
      transactionContextAccessLock.unlock();
    }
  }

  @Override
  public void clear(QueueStore queue) throws InterruptedException {
    transactionContextAccessLock.lock();
    try {
      delegate.clear(queue);
    } finally {
      transactionContextAccessLock.unlock();
    }
  }

  @Override
  public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException {
    long beginMillis = currentTimeMillis();
    final boolean lockAcquired = transactionContextAccessLock.tryLock(pollTimeout, MILLISECONDS);
    if (lockAcquired) {
      try {
        long remainingTimeout = getRemainingTimeout(pollTimeout, beginMillis);
        if (remainingTimeout >= 0) {
          return delegate.poll(queue, remainingTimeout);
        }
      } finally {
        transactionContextAccessLock.unlock();
      }
    }
    return null;
  }

  @Override
  public Serializable peek(QueueStore queue) throws InterruptedException {
    transactionContextAccessLock.lock();
    try {
      return delegate.peek(queue);
    } finally {
      transactionContextAccessLock.unlock();
    }
  }

  @Override
  public int size(QueueStore queue) {
    transactionContextAccessLock.lock();
    try {
      return delegate.size(queue);
    } finally {
      transactionContextAccessLock.unlock();
    }
  }

  private long getRemainingTimeout(long originalTimeout, long beginMillis) {
    if (originalTimeout == 0) {
      // Zero may have special behavior in some implementation of queue methods, like disable the timeout or return immediately
      return 0;
    }
    long elapsedMillis = currentTimeMillis() - beginMillis;
    return originalTimeout - elapsedMillis;
  }
}
