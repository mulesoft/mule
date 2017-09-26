/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.runtime.core.internal.transaction.xa.AbstractResourceManager;
import org.mule.runtime.core.internal.transaction.xa.AbstractTransactionContext;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.internal.util.xa.XaTransactionRecoverer;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * A Queue session that is used to manage the transaction context of a Queue.
 * <p/>
 * This QueueSession can be used for local and xa transactions
 */
public class TransactionalQueueSession extends AbstractQueueSession implements QueueSession {

  private final QueueXaResource queueXaResource;
  private final AbstractResourceManager resourceManager;
  private final LocalTxQueueTransactionJournal localTxTransactionJournal;
  private final ReentrantReadWriteLock txContextReadWriteLock;
  private LocalQueueTransactionContext singleResourceTxContext;

  public TransactionalQueueSession(QueueProvider queueProvider, QueueXaResourceManager xaResourceManager,
                                   AbstractResourceManager resourceManager, XaTransactionRecoverer xaTransactionRecoverer,
                                   LocalTxQueueTransactionJournal localTxTransactionJournal, MuleContext muleContext) {
    super(queueProvider, muleContext);
    this.localTxTransactionJournal = localTxTransactionJournal;
    this.resourceManager = resourceManager;
    this.queueXaResource = new QueueXaResource(xaResourceManager, xaTransactionRecoverer, getQueueProvider());
    this.txContextReadWriteLock = new ReentrantReadWriteLock();
  }

  protected QueueTransactionContext getTransactionalContext() {
    if (singleResourceTxContext != null) {
      return singleResourceTxContext;
    } else {
      return queueXaResource.getTransactionContext();
    }
  }

  // Local transaction implementation
  public void begin() throws ResourceManagerException {
    final ReentrantReadWriteLock.WriteLock writeLock = txContextReadWriteLock.writeLock();
    writeLock.lock();
    try {
      if (getTransactionalContext() != null) {
        throw new IllegalStateException("Cannot start local transaction. A local transaction already in progress.");
      }
      singleResourceTxContext =
          new LocalTxQueueTransactionContext(localTxTransactionJournal, getQueueProvider(), txContextReadWriteLock.readLock());
      resourceManager.beginTransaction((AbstractTransactionContext) singleResourceTxContext);
    } finally {
      writeLock.unlock();
    }
  }

  public void commit() throws ResourceManagerException {
    final ReentrantReadWriteLock.WriteLock writeLock = txContextReadWriteLock.writeLock();
    writeLock.lock();
    try {
      if (singleResourceTxContext == null) {
        throw new IllegalStateException("Cannot commit local transaction as no transaction was begun");
      }
      resourceManager.commitTransaction((AbstractTransactionContext) singleResourceTxContext);
      singleResourceTxContext = null;
    } finally {
      writeLock.unlock();
    }
  }

  public void rollback() throws ResourceManagerException {
    final ReentrantReadWriteLock.WriteLock writeLock = txContextReadWriteLock.writeLock();
    writeLock.lock();
    try {
      if (singleResourceTxContext == null) {
        throw new IllegalStateException("Cannot commit local transaction as no transaction was begun");
      }
      resourceManager.rollbackTransaction((AbstractTransactionContext) singleResourceTxContext);
      singleResourceTxContext = null;
    } finally {
      writeLock.unlock();
    }
  }

  // XA transaction delegation to QueueXaResource
  @Override
  public boolean isSameRM(XAResource xares) throws XAException {
    return queueXaResource.isSameRM(xares);
  }

  @Override
  public void start(Xid xid, int flags) throws XAException {
    queueXaResource.start(xid, flags);
  }

  @Override
  public void end(Xid xid, int flags) throws XAException {
    queueXaResource.end(xid, flags);
  }

  @Override
  public void commit(Xid xid, boolean onePhase) throws XAException {
    queueXaResource.commit(xid, onePhase);
  }

  @Override
  public void rollback(Xid xid) throws XAException {
    queueXaResource.rollback(xid);
  }

  @Override
  public int prepare(Xid xid) throws XAException {
    return queueXaResource.prepare(xid);
  }

  @Override
  public void forget(Xid xid) throws XAException {
    queueXaResource.forget(xid);
  }

  @Override
  public int getTransactionTimeout() throws XAException {
    return queueXaResource.getTransactionTimeout();
  }

  @Override
  public boolean setTransactionTimeout(int timeout) throws XAException {
    return queueXaResource.setTransactionTimeout(timeout);
  }

  @Override
  public Xid[] recover(int i) throws XAException {
    return queueXaResource.recover(i);
  }
}
