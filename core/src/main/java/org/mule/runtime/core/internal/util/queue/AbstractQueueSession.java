/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueSession;

public abstract class AbstractQueueSession implements QueueSession {

  private final QueueProvider queueProvider;
  private final MuleContext muleContext;

  public AbstractQueueSession(QueueProvider queueProvider, MuleContext muleContext) {
    this.queueProvider = queueProvider;
    this.muleContext = muleContext;
  }

  @Override
  public Queue getQueue(String name) {
    QueueStore queueStore = queueProvider.getQueue(name);
    return new TransactionAwareQueueStore(queueStore, new TransactionContextProvider() {

      @Override
      public boolean isTransactional() {
        return getTransactionalContext() != null;
      }

      @Override
      public QueueTransactionContext getTransactionalContext() {
        return AbstractQueueSession.this.getTransactionalContext();
      }
    }, muleContext);
  }

  protected QueueProvider getQueueProvider() {
    return queueProvider;
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

  protected abstract QueueTransactionContext getTransactionalContext();
}
