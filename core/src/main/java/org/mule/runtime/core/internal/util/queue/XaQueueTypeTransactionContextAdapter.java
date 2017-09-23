/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.runtime.core.internal.transaction.xa.AbstractXaTransactionContext;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import java.io.Serializable;

import javax.transaction.xa.Xid;

public class XaQueueTypeTransactionContextAdapter extends AbstractXaTransactionContext
    implements XaQueueTransactionContext, QueueTransactionContextFactory<XaQueueTransactionContext> {

  private final XaTxQueueTransactionJournal xaTxQueueTransactionJournal;
  private final QueueProvider queueProvider;
  private final QueueTypeTransactionContextAdapter<XaQueueTransactionContext> delegate;
  private final Xid xid;

  public XaQueueTypeTransactionContextAdapter(XaTxQueueTransactionJournal xaTxQueueTransactionJournal,
                                              QueueProvider queueProvider, Xid xid) {
    this.xaTxQueueTransactionJournal = xaTxQueueTransactionJournal;
    this.queueProvider = queueProvider;
    this.xid = xid;
    this.delegate = new QueueTypeTransactionContextAdapter(this);
  }

  @Override
  public XaQueueTransactionContext createPersistentTransactionContext() {
    return new PersistentXaTransactionContext(xaTxQueueTransactionJournal, queueProvider, xid);
  }

  @Override
  public XaQueueTransactionContext createTransientTransactionContext() {
    return new TransientXaTransactionAdapter(new TransientQueueTransactionContext());
  }

  @Override
  public void doCommit() throws ResourceManagerException {
    XaQueueTransactionContext transactionContext = delegate.getTransactionContext();
    if (transactionContext != null) {
      transactionContext.doCommit();
    }
  }

  @Override
  public void doRollback() throws ResourceManagerException {
    XaQueueTransactionContext transactionContext = delegate.getTransactionContext();
    if (transactionContext != null) {
      transactionContext.doRollback();
    }
  }

  @Override
  public void doPrepare() throws ResourceManagerException {
    XaQueueTransactionContext transactionContext = delegate.getTransactionContext();
    if (transactionContext != null) {
      transactionContext.doPrepare();
    }
  }

  @Override
  public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException {
    return delegate.offer(queue, item, offerTimeout);
  }

  @Override
  public void untake(QueueStore queue, Serializable item) throws InterruptedException {
    delegate.untake(queue, item);
  }

  @Override
  public void clear(QueueStore queue) throws InterruptedException {
    delegate.clear(queue);
  }

  @Override
  public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException {
    return delegate.poll(queue, pollTimeout);
  }

  @Override
  public Serializable peek(QueueStore queue) throws InterruptedException {
    return delegate.peek(queue);
  }

  @Override
  public int size(QueueStore queue) {
    return delegate.size(queue);
  }
}
