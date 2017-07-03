/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import java.io.Serializable;

/**
 * Adapter for {@link TransientQueueTransactionContext} to an {@link XaQueueTransactionContext}
 */
public class TransientXaTransactionAdapter implements XaQueueTransactionContext {

  private final TransientQueueTransactionContext adaptedTransactionContext;

  public TransientXaTransactionAdapter(TransientQueueTransactionContext transactionContext) {
    this.adaptedTransactionContext = transactionContext;
  }

  @Override
  public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException {
    return adaptedTransactionContext.offer(queue, item, offerTimeout);
  }

  @Override
  public void untake(QueueStore queue, Serializable item) throws InterruptedException {
    adaptedTransactionContext.untake(queue, item);
  }

  @Override
  public void clear(QueueStore queue) throws InterruptedException {
    adaptedTransactionContext.clear(queue);
  }

  @Override
  public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException {
    return adaptedTransactionContext.poll(queue, pollTimeout);
  }

  @Override
  public Serializable peek(QueueStore queue) throws InterruptedException {
    return adaptedTransactionContext.peek(queue);
  }

  @Override
  public int size(QueueStore queue) {
    return adaptedTransactionContext.size(queue);
  }

  @Override
  public void doCommit() throws ResourceManagerException {
    adaptedTransactionContext.doCommit();
  }

  @Override
  public void doRollback() throws ResourceManagerException {
    adaptedTransactionContext.doRollback();
  }

  @Override
  public void doPrepare() throws ResourceManagerException {
    // Nothing to do.
  }

}
